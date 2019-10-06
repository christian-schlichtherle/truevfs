/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.logging._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsSyncOption._
import net.java.truevfs.kernel.spec.FsSyncOptions._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Type._

/** A selective cache for file system entries.
  * Decorating a file system controller with this class has the following
  * effects:
  *
  * - Caching and buffering for an entry needs to get activated by using the
  * methods `input` or `output` with the access option
  * [[net.java.truevfs.kernel.spec.FsAccessOption.CACHE]].
  * - Unless a write operation succeeds, upon each read operation the entry
  * data gets copied from the backing store for buffering purposes only.
  * - Upon a successful write operation, the entry data gets cached for
  * subsequent read operations until the file system gets `sync`ed again.
  * - Entry data written to the cache is not written to the backing store
  * until the file system gets `sync`ed - this is a ''write back'' strategy.
  * - As a side effect, caching decouples the underlying storage from its
  * clients, allowing it to create, read, update or delete the entry data
  * while some clients are still busy on reading or writing the copied
  * entry data.
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
private trait CacheController[E <: FsArchiveEntry] extends ArchiveController[E] {
  controller: ArchiveModelAspect[E] =>

  import CacheController._

  protected def pool: IoBufferPool

  private[this] val caches = new java.util.HashMap[FsNodeName, EntryCache]

  abstract override def input(options: AccessOptions, name: FsNodeName): AnyInputSocket = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    new DelegatingInputSocket[Entry] {
      override def socket(): AnyInputSocket = {
        assert(writeLockedByCurrentThread)
        var cache = caches get name
        if (null eq cache) {
          if (!(options get CACHE)) {
            return CacheController.super.input(options, name)
          }
          cache = new EntryCache(name)
        }
        cache input options
      }
    }
  }

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]): AnyOutputSocket = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    new DelegatingOutputSocket[Entry] {
      override def socket(): AnyOutputSocket = {
        assert(writeLockedByCurrentThread)
        var cache = caches get name
        if (null eq cache) {
          if (!(options get CACHE)) {
            return CacheController.super.output(options, name, template)
          }
          cache = new EntryCache(name)
        }
        cache.output(options, template)
      }
    }
  }

  abstract override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]): Unit = {
    assert(writeLockedByCurrentThread)
    super.make(options, name, tµpe, template)
    val cache = caches remove name
    if (null ne cache) {
      cache.clear()
    }
  }

  abstract override def unlink(options: AccessOptions, name: FsNodeName): Unit = {
    assert(writeLockedByCurrentThread)
    super.unlink(options, name)
    val cache = caches remove name
    if (null ne cache) {
      cache.clear()
    }
  }

  abstract override def sync(options: SyncOptions): Unit = {
    assert(writeLockedByCurrentThread)
    assert(!readLockedByCurrentThread)

    syncCacheEntries(options)
    super.sync(options.clear(CLEAR_CACHE))
    if (caches.isEmpty) mounted = false
  }

  private def syncCacheEntries(options: SyncOptions): Unit = {
    // HC SVNT DRACONES!
    if (0 >= caches.size()) return
    val flush = !(options get ABORT_CHANGES)
    val clear = !flush || (options get CLEAR_CACHE)
    assert(flush || clear)
    val builder = new FsSyncExceptionBuilder
    val i = caches.values.iterator
    while (i.hasNext) {
      val cache = i.next()
      if (flush) {
        try {
          cache.flush()
        } catch {
          case ex: IOException =>
            throw builder fail new FsSyncException(mountPoint, ex)
        }
      }
      if (clear) {
        i.remove()
        try {
          cache.clear()
        } catch {
          case ex: IOException =>
            builder warn new FsSyncWarningException(mountPoint, ex)
        }
      }
    }
    builder.check()
  }

  /** A cache for the contents of an individual archive entry. */
  private final class EntryCache(val name: FsNodeName) {

    private val cache: CacheEntry = CacheEntry.Strategy.WriteBack.newCacheEntry(pool)

    def flush(): Unit = {
      cache.flush()
    }

    def clear(): Unit = {
      cache.release()
    }

    def register(): Unit = {
      caches.put(name, this)
    }

    def input(options: AccessOptions): AnyInputSocket = {

      /** This class requires LAZY INITIALIZATION of its channel, but NO
        * automatic decoupling on exceptions!
        */
      class Input extends DelegatingInputSocket[Entry] {

        private[this] val _options = options clear CACHE // consume

        override lazy val socket: AnyInputSocket = CacheController.super.input(_options, name)

        override def stream(peer: AnyOutputSocket): InputStream = {
          assert(writeLockedByCurrentThread)

          new DecoratingInputStream(socket stream peer) {
            assert(mounted)

            override def close(): Unit = {
              assert(writeLockedByCurrentThread)
              in.close()
              register()
            }
          }
        }

        override def channel(peer: AnyOutputSocket) = throw new AssertionError
      }

      cache.configure(new Input).input
    }

    def output(options: AccessOptions, template: Option[Entry]): AnyOutputSocket = {

      /** This class requires LAZY INITIALIZATION of its channel, but NO
        * automatic decoupling on exceptions!
        */
      class Output extends DelegatingOutputSocket[Entry] {

        private[this] val _options = options clear CACHE // consume

        override lazy val socket: AnyOutputSocket = cache
          .configure(CacheController.super.output(_options clear EXCLUSIVE, name, template))
          .output

        override def stream(peer: AnyInputSocket): OutputStream = {
          assert(writeLockedByCurrentThread)
          preOutput()

          new DecoratingOutputStream(socket stream peer) {
            register()

            override def close(): Unit = {
              assert(writeLockedByCurrentThread)
              out.close()
              postOutput()
            }
          }
        }

        override def channel(peer: AnyInputSocket): SeekableByteChannel = {
          assert(writeLockedByCurrentThread)
          preOutput()

          new DecoratingSeekableChannel(socket channel peer) {
            register()

            override def close(): Unit = {
              assert(writeLockedByCurrentThread)
              channel.close()
              postOutput()
            }
          }
        }

        def preOutput(): Unit = {
          make(_options, template)
        }

        def postOutput(): Unit = {
          make(_options clear EXCLUSIVE, template orElse Option(cache))
          register()
        }

        def make(options: AccessOptions, template: Option[Entry]): Unit = {
          var makeOpts = options
          while (true) {
            try {
              CacheController.super.make(makeOpts, name, FILE, template)
              return
            } catch {
              case makeEx: NeedsSyncException =>
                // In this context, this exception means that the entry
                // has already been written to the output archive for
                // the target archive file.

                // Pass on the exception if there is no means to
                // resolve the issue locally, that is if we were asked
                // to create the entry exclusively or this is a
                // non-recursive file system operation.
                if (makeOpts get EXCLUSIVE) throw makeEx
                val syncOpts = SyncController modify SYNC
                if (SYNC eq syncOpts) throw makeEx

                // Try to resolve the issue locally.
                // Even if we were asked to create the entry EXCLUSIVEly, we
                // first need to try to get the cache in sync() with the
                // virtual file system again and retry the make().
                try {
                  CacheController.super.sync(syncOpts)
                  // sync() succeeded, now repeat the make()
                } catch {
                  case syncEx: FsSyncException =>
                    syncEx addSuppressed makeEx

                    // sync() failed, maybe just because the current
                    // thread has already acquired some open I/O
                    // resources for the same target archive file, e.g.
                    // an input stream for a copy operation and this
                    // is an artifact of an attempt to acquire the
                    // output stream for a child file system.
                    syncEx.getCause match {
                      case _: FsOpenResourceException =>
                        // OK, we couldn't sync() because the current
                        // thread has acquired open I/O resources for the
                        // same target archive file.
                        // Normally, we would be expected to rethrow the
                        // make exception to trigger another sync(), but
                        // this would fail for the same reason und create
                        // an endless loop, so we can't do this.
                        //throw mknodEx;

                        // Dito for mapping the exception.
                        //throw FsNeedsLockRetryException.get(getModel());

                        // Check if we can retry the make with GROW set.
                        val oldMknodOpts = makeOpts
                        makeOpts = oldMknodOpts set GROW
                        if (makeOpts eq oldMknodOpts) {
                          // Finally, the make failed because the entry
                          // has already been output to the target archive
                          // file - so what?!
                          // This should mark only a volatile issue because
                          // the next sync() will sort it out once all the
                          // I/O resources have been closed.
                          // Let's log the sync exception - mind that it has
                          // suppressed the make exception - and continue
                          // anyway...
                          logger debug("ignoring", syncEx)
                          return
                        }
                      case _ =>
                        // Too bad, sync() failed because of a more
                        // serious issue than just some open resources.
                        // Let's rethrow the sync exception.
                        throw syncEx
                    }
                }
            }
          }
          assert(false)
        }
      }

      new Output
    }
  }

}

private object CacheController {

  val logger = new LocalizedLogger(classOf[CacheController[_]])
}
