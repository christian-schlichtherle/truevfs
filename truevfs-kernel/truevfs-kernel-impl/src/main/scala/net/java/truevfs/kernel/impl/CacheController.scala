/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.logging._
import net.java.truecommons.shed._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsSyncOption._
import net.java.truevfs.kernel.spec.FsSyncOptions._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._;
import net.java.truevfs.kernel.spec.cio.Entry.Type._;

/** A selective cache for file system entries.
  * Decorating a file system controller with this class has the following
  * effects:
  * 
  * - Caching and buffering for an entry needs to get activated by using the
  *   methods `input` or `output` with the access option
  *   [[net.java.truevfs.kernel.impl.FsAccessOption.CACHE]].
  * - Unless a write operation succeeds, upon each read operation the entry
  *   data gets copied from the backing store for buffering purposes only.
  * - Upon a successful write operation, the entry data gets cached for
  *   subsequent read operations until the file system gets `sync`ed again.
  * - Entry data written to the cache is not written to the backing store
  *   until the file system gets `sync`ed - this is a ''write back'' strategy.
  * - As a side effect, caching decouples the underlying storage from its
  *   clients, allowing it to create, read, update or delete the entry data
  *   while some clients are still busy on reading or writing the copied
  *   entry data.
  * 
  * '''TO THE FUTURE ME:'''
  * FOR TRUEZIP 7.5, IT TOOK ME TWO MONTHS OF CONSECUTIVE CODING, TESTING,
  * DEBUGGING, ANALYSIS AND SWEATING TO GET THIS DAMN BEAST WORKING STRAIGHT!
  * DON'T EVEN THINK YOU COULD CHANGE A SINGLE CHARACTER IN THIS CODE AND
  * EASILY GET AWAY WITH IT!
  * '''YOU HAVE BEEN WARNED!'''
  * 
  * Well, if you really feel like changing something, run the integration test
  * suite at least ten times to make sure your changes really work - I mean it!
  * 
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private trait CacheController[E <: FsArchiveEntry]
extends ArchiveController[E] {
  controller: ArchiveModelAspect[E] =>

  protected def pool: IoBufferPool

  private[this] val caches = new java.util.HashMap[FsNodeName, EntryCache]

  abstract override def input(options: AccessOptions, name: FsNodeName) = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    class Input extends DelegatingInputSocket[Entry] {
      override def socket(): AnyInputSocket = {
        assert(writeLockedByCurrentThread)
        var cache = caches get name
        if (null eq cache) {
            if (!(options get CACHE))
              return CacheController.super.input(options, name)
          cache = new EntryCache(name)
        }
        cache input options
      }
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]) = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    class Output extends DelegatingOutputSocket[Entry] {
      override def socket(): AnyOutputSocket = {
        assert(writeLockedByCurrentThread)
        var cache = caches get name
        if (null eq cache) {
            if (!(options get CACHE))
              return CacheController.super.output(options, name, template)
          cache = new EntryCache(name)
        }
        cache output (options, template)
      }
    }
    new Output
  }: AnyOutputSocket

  abstract override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) {
    assert(writeLockedByCurrentThread)
    super.make(options, name, tµpe, template)
    val cache = caches remove name
    if (null ne cache) cache clear ()
  }

  abstract override def unlink(options: AccessOptions, name: FsNodeName) {
    assert(writeLockedByCurrentThread)
    super.unlink(options, name)
    val cache = caches remove name
    if (null ne cache) cache clear()
  }

  abstract override def sync(options: SyncOptions) {
    syncCacheEntries(options)
    super.sync(options.clear(CLEAR_CACHE))
    if (caches.isEmpty) mounted = false
  }

  private def syncCacheEntries(options: SyncOptions) {
    assert(writeLockedByCurrentThread)
    // HC SVNT DRACONES!
    if (0 >= caches.size()) return
    val flush = !(options get ABORT_CHANGES)
    val clear = !flush || (options get CLEAR_CACHE)
    assert(flush || clear)
    val builder = new FsSyncExceptionBuilder
    val i = caches.values.iterator
    while (i.hasNext) {
      val cache = i next ()
      if (flush) {
        try {
          cache flush ()
        } catch {
          case ex: IOException =>
            throw builder fail new FsSyncException(mountPoint, ex)
        }
      }
      if (clear) {
        i remove ()
        try {
          cache clear ()
        } catch {
          case ex: IOException =>
            builder warn new FsSyncWarningException(mountPoint, ex)
        }
      }
    }
    builder check ()
  }

  /** A cache for the contents of an individual archive entry. */
  private final class EntryCache(val name: FsNodeName) {
    val cache = CacheEntry.Strategy.WriteBack.newCacheEntry(pool)

    def flush() { cache flush () }

    def clear() { cache release () }

    def register() { caches put (name, this) }

    def input(options: AccessOptions) = {
      /** This class requires LAZY INITIALIZATION of its channel, but NO
        * automatic decoupling on exceptions!
        */
      final class Input extends DelegatingInputSocket[Entry] {
        private[this] val _options = options clear CACHE // consume

        override lazy val socket = CacheController.super.input(_options, name)

        override def stream(peer: AnyOutputSocket) = {
          assert(writeLockedByCurrentThread)

          final class Stream extends DecoratingInputStream(socket stream peer) {
            assert(mounted)

            override def close() {
              assert(writeLockedByCurrentThread)
              in close ()
              register()
            }
          }
          new Stream
        }

        override def channel(peer: AnyOutputSocket) = throw new AssertionError
      }
      cache.configure(new Input).input
    }

    def output(options: AccessOptions, template: Option[Entry]) = {
      /** This class requires LAZY INITIALIZATION of its channel, but NO
        * automatic decoupling on exceptions!
        */
      final class Output extends DelegatingOutputSocket[Entry] {
        private[this] val _options = options clear CACHE // consume

        override lazy val socket = cache
          .configure(CacheController.super.output(
              _options clear EXCLUSIVE, name, template))
          .output

        override def stream(peer: AnyInputSocket) = {
          assert(writeLockedByCurrentThread)
          preOutput()

          final class Stream extends DecoratingOutputStream(socket stream peer) {
            register()

            override def close() {
              assert(writeLockedByCurrentThread)
              out close ()
              postOutput()
            }
          }
          new Stream
        }

        override def channel(peer: AnyInputSocket) = {
          assert(writeLockedByCurrentThread)
          preOutput()

          final class Channel extends DecoratingSeekableChannel(socket channel peer) {
            register()

            override def close() {
              assert(writeLockedByCurrentThread)
              channel close ()
              postOutput()
            }
          }
          new Channel
        }

        def preOutput() { make(_options, template) }

        def postOutput() {
          make(_options clear EXCLUSIVE, template orElse Option(cache)) 
          register()
        }

        def make(options: AccessOptions, template: Option[Entry]) {
          CacheController.super.make(options, name, FILE, template)
        }
      } // Output
      new Output
    }
  } // EntryCache
}
