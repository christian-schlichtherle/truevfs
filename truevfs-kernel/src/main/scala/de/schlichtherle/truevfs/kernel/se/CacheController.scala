/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import net.truevfs.kernel.FsAccessOption._
import net.truevfs.kernel.FsSyncOption._
import net.truevfs.kernel.FsSyncOptions._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.cio.Entry.Type._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._
import java.util.logging._
import scala.util.control.Breaks

/**
 * A selective cache for file system entries.
 * Decorating a file system controller with this class has the following
 * effects:
 * <ul>
 * <li>Caching and buffering for an entry needs to get activated by using the
 *     method
 *     {@link #input input socket} with the input option
 *     {@link FsAccessOption#CACHE} or the method
 *     {@link #output output socket} with the output option
 *     {@link FsAccessOption#CACHE}.
 * <li>Unless a write operation succeeds, upon each read operation the entry
 *     data gets copied from the backing store for buffering purposes only.
 * <li>Upon a successful write operation, the entry data gets cached for
 *     subsequent read operations until the file system gets
 *     {@link #sync synced} again.
 * <li>Entry data written to the cache is not written to the backing store
 *     until the file system gets {@link #sync synced} - this is a
 *     <i>write back</i> strategy.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the copied
 *     entry data.
 * </ul>
 * <p>
 * <strong>TO THE FUTURE ME:</strong>
 * FOR TRUEZIP 7.5, IT TOOK ME TWO MONTHS OF CONSECUTIVE CODING, TESTING,
 * DEBUGGING, ANALYSIS AND SWEATING TO GET THIS DAMN BEAST WORKING STRAIGHT!
 * DON'T EVEN THINK YOU COULD CHANGE A SINGLE CHARACTER IN THIS CODE AND EASILY
 * GET AWAY WITH IT!
 * <strong>YOU HAVE BEEN WARNED!</strong>
 * <p>
 * Well, if you really feel like changing something, run the integration test
 * suite at least ten times to make sure your changes really work - I mean it!
 * 
 * @author Christian Schlichtherle
 */
private trait CacheController
extends FsController[LockModel]
with LockModelController {
  import CacheController._

  protected def pool: IOPool[_]

  private[this] val caches = new java.util.HashMap[FsEntryName, EntryCache]

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    final class Input extends DelegatingInputSocket[Entry] {
      override def socket(): AnyInputSocket = {
        assert(isWriteLockedByCurrentThread)
        var cache = caches.get(name)
        if (null eq cache) {
            if (!options.get(CACHE))
              return CacheController.super.input(options, name)
          //checkWriteLockedByCurrentThread();
          cache = new EntryCache(name);
        }
        cache.input(options)
      }
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Entry) = {
    /** This class requires ON-DEMAND LOOKUP of its delegate socket! */
    final class Output extends DelegatingOutputSocket[Entry] {
      override def socket(): AnyOutputSocket = {
        assert(isWriteLockedByCurrentThread)
        var cache = caches.get(name)
        if (null eq cache) {
            if (!options.get(CACHE))
              return CacheController.super.output(options, name, template)
          //checkWriteLockedByCurrentThread();
          cache = new EntryCache(name);
        }
        cache.output(options, template)
      }
    }
    new Output
  }: AnyOutputSocket

  abstract override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Entry) {
    assert(isWriteLockedByCurrentThread)
    super.mknod(options, name, tµpe, template)
    val cache = caches.remove(name)
    if (null ne cache)
      cache.release()
  }

  abstract override def unlink(options: AccessOptions, name: FsEntryName) {
    assert(isWriteLockedByCurrentThread)
    super.unlink(options, name)
    val cache = caches.remove(name)
    if (null ne cache)
      cache.release()
  }

  abstract override def sync(options: SyncOptions) {
    assert(isWriteLockedByCurrentThread)
    var preSyncEx: NeedsSyncException = null
    do {
      preSyncEx = null;
      try {
          preSync(options)
      } catch {
        case invalidState: NeedsSyncException =>
          // The target archive controller is in an invalid state because
          // it reports to need a sync() while the current thread is
          // actually doing a sync().
          // This is expected to be a volatile event which may have been
          // caused by the following scenario:
          // Another thread attempted to sync() the nested target archive
          // file but initially failed because the parent file system
          // controller has thrown an FsNeedsLockRetryException when
          // trying to close() the input or output resources for the
          // target archive file.
          // The other thread has then released all its file system write
          // locks and is now retrying the operation but lost the race
          // for the file system write lock against this thread which has
          // now detected the invalid state.

          // In an attempt to recover from this invalid state, the
          // current thread could just step back in order to give the
          // other thread a chance to complete its sync().
          //throw FsNeedsLockRetryException.get(getModel());

          // However, this would unnecessarily defer the current thread
          // and might result in yet another thread to discover the
          // invalid state, which reduces the overall performance.
          // So instead, the current thread will now attempt to resolve
          // the invalid state by sync()ing the target archive controller
          // before preSync()ing the cache again.
          logger.log(Level.FINE, "recovering", invalidState);
          preSyncEx = invalidState; // trigger another iteration
      }
      // TODO: Consume FsSyncOption.CLEAR_CACHE and release a flag in
      // the model instead.
      super.sync(options/*.clear(CLEAR_CACHE)*/)
    } while (null ne preSyncEx)
  }

  private def preSync(options: SyncOptions) {
    if (0 >= caches.size())
      return
    val flush = !options.get(ABORT_CHANGES)
    var release = !flush || options.get(CLEAR_CACHE)
    assert(flush || release)
    val builder = new FsSyncExceptionBuilder
    val i = caches.values.iterator
    while (i.hasNext) {
      val cache = i.next()
      try {
        if (flush) {
          try {
            cache.flush()
          } catch {
            case ex: IOException =>
              throw builder fail new FsSyncException(getModel, ex)
          }
        }
      } catch {
        case ex: Throwable =>
          release = false
          throw ex
      } finally {
        if (release) {
          i.remove()
          try {
            cache.release()
          } catch {
            case ex: IOException =>
              builder warn new FsSyncWarningException(getModel, ex)
          }
        }
      }
    }
    builder.check()
  }

  /** A cache for the contents of an individual archive entry. */
  private final class EntryCache(val name: FsEntryName) {
    val cache = CacheEntry.Strategy.WRITE_BACK.newCache(pool)

    def flush() { cache.flush() }

    def release() { cache.release() }

    def register() { caches.put(name, this) }

    def input(options: AccessOptions) = {
      /**
       * This class requires LAZY INITIALIZATION of its channel, but NO
       * automatic decoupling on exceptions!
       */
      final class Input extends ClutchInputSocket[Entry] {
        private[this] val o = options.clear(CACHE) // consume

        override def lazySocket = CacheController.super.input(o, name)

        // Bypass the super class implementation to keep the
        // socket even upon an exception!
        override def localTarget() = boundSocket.localTarget()

        override def stream() = {
          assert(isWriteLockedByCurrentThread)

          // Bypass the super class implementation to keep the
          // socket even upon an exception!
          final class Stream extends DecoratingInputStream(boundSocket.stream()) {
            assert(getModel.isTouched)

            override def close() {
              assert(isWriteLockedByCurrentThread)
              in.close()
              register()
            }
          }
          new Stream
        }

        override def channel() = throw new AssertionError
      }
      cache.configure(new Input()).input
    }: AnyInputSocket

    def output(options: AccessOptions, template: Entry) = {
      /**
       * This class requires LAZY INITIALIZATION of its channel, but NO
       * automatic decoupling on exceptions!
       */
      final class Output extends ClutchOutputSocket[Entry] {
        private[this] val o = options.clear(CACHE) // consume

        override def lazySocket = cache
          .configure(CacheController.super.output(
              o.clear(EXCLUSIVE), name, template))
          .output

        // Bypass the super class implementation to keep the
        // socket even upon an exception!
        override def localTarget() = boundSocket.localTarget()

        override def stream() = {
          assert(isWriteLockedByCurrentThread)
          preOutput()

          // Bypass the super class implementation to keep the
          // socket even upon an exception!
          final class Stream extends DecoratingOutputStream(boundSocket.stream()) {
            register()

            override def close() {
              assert(isWriteLockedByCurrentThread)
              out.close()
              postOutput()
            }
          }
          new Stream
        }

        override def channel() = {
          assert(isWriteLockedByCurrentThread)
          preOutput()

          // Bypass the super class implementation to keep the
          // socket even upon an exception!
          final class Channel extends DecoratingSeekableChannel(boundSocket.channel()) {
            register()

            override def close() {
              assert(isWriteLockedByCurrentThread)
              channel.close()
              postOutput()
            }
          }
          new Channel
        }

        def preOutput() { mknod(o, template) }

        def postOutput() {
          mknod(o.clear(EXCLUSIVE), Option(template).getOrElse(cache)) 
          register()
        }

        def mknod(options: AccessOptions, template: Entry) {
          var mknodOpts = options
          val breaks = new Breaks
          import breaks.{break, breakable}
          breakable {
            while (true) {
              try {
                CacheController.super.mknod(mknodOpts, name, FILE, template)
                break
              } catch {
                case mknodEx: NeedsSyncException =>
                  // In this context, this exception means that the entry
                  // has already been written to the output archive for
                  // the target archive file.

                  // Pass on the exception if there is no means to
                  // resolve the issue locally, that is if we were asked
                  // to create the entry exclusively or this is a
                  // non-recursive file system operation.
                  if (mknodOpts.get(EXCLUSIVE))
                    throw mknodEx;
                  val syncOpts = SyncController.modify(SYNC)
                  if (SYNC eq syncOpts)
                    throw mknodEx;

                  // Try to resolve the issue locally.
                  // Even if we were asked to create the entry
                  // EXCLUSIVEly, first we must try to get the cache in
                  // sync() with the virtual file system again and retry
                  // the mknod().
                  try {
                    CacheController.super.sync(syncOpts);
                    //continue; // sync() succeeded, now repeat mknod()
                  } catch {
                    case syncEx: FsSyncException =>
                      syncEx.addSuppressed(mknodEx);

                      // sync() failed, maybe just because the current
                      // thread has already acquired some open I/O
                      // resources for the same target archive file, e.g.
                      // an input stream for a copy operation and this
                      // is an artifact of an attempt to acquire the
                      // output stream for a child file system.
                      syncEx.getCause match {
                        case _: FsResourceOpenException =>
                        case _ =>
                          // Too bad, sync() failed because of a more
                          // serious issue than just some open resources.
                          // Let's rethrow the sync exception.
                          throw syncEx;
                      }

                      // OK, we couldn't sync() because the current
                      // thread has acquired open I/O resources for the
                      // same target archive file.
                      // Normally, we would be expected to rethrow the
                      // mknod exception to trigger another sync(), but
                      // this would fail for the same reason und create
                      // an endless loop, so we can't do this.
                      //throw mknodEx;

                      // Dito for mapping the exception.
                      //throw FsNeedsLockRetryException.get(getModel());

                      // Check if we can retry the mknod with GROW set.
                      val oldMknodOpts = mknodOpts
                      mknodOpts = oldMknodOpts.set(GROW)
                      if (mknodOpts eq oldMknodOpts) {
                          // Finally, the mknod failed because the entry
                          // has already been output to the target archive
                          // file - so what?!
                          // This should mark only a volatile issue because
                          // the next sync() will sort it out once all the
                          // I/O resources have been closed.
                          // Let's log the sync exception - mind that it has
                          // suppressed the mknod exception - and continue
                          // anyway...
                          logger.log(Level.FINE, "ignoring", syncEx);
                          break
                      }
                  }
              }
            }
          }
        }
      }
      new Output
    }: AnyOutputSocket
  } // EntryCache
}

private object CacheController {
  private val logger = Logger.getLogger(
    classOf[CacheController].getName,
    classOf[CacheController].getName);
}
