/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec.FsSyncOption._
import net.truevfs.kernel.spec.cio._
import net.truevfs.kernel.spec.cio.Entry._;
import net.truevfs.kernel.spec.io._
import net.truevfs.kernel.spec.util._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import ResourceAccountant.Resources

/** Accounts input and output resources returned by its decorated controller.
  * 
  * @see    ResourceManager
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private trait ResourceController extends Controller[LockModel] {
  this: LockModelAspect =>

  import ResourceController._

  private[this] val accountant = new ResourceAccountant(writeLock)

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends DelegatingInputSocket[Entry] {
      val socket = ResourceController.super.input(options, name)

      override def stream(peer: AnyOutputSocket) =
        new ResourceInputStream(socket.stream(peer))

      override def channel(peer: AnyOutputSocket) =
        new ResourceSeekableChannel(socket.channel(peer))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Option[Entry]) = {
    final class Output extends DelegatingOutputSocket[Entry] {
      val socket = ResourceController.super.output(options, name, template)

      override def stream(peer: AnyInputSocket) =
        new ResourceOutputStream(socket.stream(peer))

      override def channel(peer: AnyInputSocket) =
        new ResourceSeekableChannel(socket.channel(peer))
    }
    new Output
  }: AnyOutputSocket

  abstract override def sync(options: SyncOptions) {
    assert(writeLockedByCurrentThread)
    val builder = new FsSyncExceptionBuilder
    waitIdle(options, builder)
    closeAll(builder)
    try { super.sync(options) }
    catch { case ex: FsSyncException => builder warn ex }
    builder check();
  }

  private def waitIdle(options: SyncOptions, builder: FsSyncExceptionBuilder) {
    try {
      waitIdle(options)
    } catch {
      case ex: FsResourceOpenException =>
        if (!(options get FORCE_CLOSE_IO))
            throw builder fail new FsSyncException(mountPoint, ex)
        builder warn new FsSyncWarningException(mountPoint, ex)
    }
  }

  private def waitIdle(options: SyncOptions) {
    // HC SVNT DRACONES!
    {
      val Resources(local, total) = accountant.resources
      if (0 != local && !(options get FORCE_CLOSE_IO))
          throw new FsResourceOpenException(local, total)
    }
    val wait = options get WAIT_CLOSE_IO
    if (!wait) {
      // Spend some effort on closing streams which have already been garbage
      // collected in order to compensates for a disadvantage of the
      // NeedsLockRetryException:
      // An FsArchiveDriver may try to close() a file system entry but fail to
      // do so because of a NeedsLockRetryException which is impossible to
      // resolve in a driver.
      // The TarDriver family is known to be affected by this.
      System.runFinalization()
    }
    accountant waitOtherThreads (if (wait) 0 else waitTimeoutMillis);
    {
      val Resources(local, total) = accountant.resources
      if (0 != total) throw new FsResourceOpenException(local, total)
    }
  }

  /** Closes and disconnects all entry streams of the output and input archive.
    * 
    * @param builder the exception handling strategy.
    */
  private def closeAll(builder: FsSyncExceptionBuilder) {
    final class IOExceptionHandler
    extends ExceptionHandler[IOException, RuntimeException] {
      def fail(ex: IOException) = throw new AssertionError(ex)
      def warn(ex: IOException) {
        builder.warn(new FsSyncWarningException(mountPoint, ex))
      }
    } // IOExceptionHandler
    accountant closeAllResources (new IOExceptionHandler)
  }

  private class ResourceInputStream(in: InputStream)
  extends DecoratingInputStream(in) with ResourceCloseable

  private class ResourceOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) with ResourceCloseable

  private class ResourceSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) with ResourceCloseable

  private trait ResourceCloseable extends Closeable {
    accountant startAccountingFor this

    abstract override def close() {
      super.close()
      accountant stopAccountingFor this
    }
  }
}

private object ResourceController {
  private val waitTimeoutMillis = LockingStrategy acquireTimeoutMillis
}
