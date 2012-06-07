/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import net.truevfs.kernel.FsSyncOption._
import net.truevfs.kernel.FsSyncOptions._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._

/** Performs a `sync` operation if required.
  * 
  * This controller is a barrier for
  * [[de.schlichtherle.truevfs.kernel.NeedsSyncException]]s:
  * Whenever the decorated controller chain throws a `NeedsSyncException`,
  * the file system gets `sync`ed before the operation gets retried.
  * 
  * @see    NeedsSyncException
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait SyncController extends Controller[LockModel] {
  this: LockModelAspect =>

  abstract override def stat(options: AccessOptions, name: FsEntryName) =
    apply(super.stat(options, name))

  abstract override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) =
    apply(super.checkAccess(options, name, types))

  abstract override def setReadOnly(name: FsEntryName) =
    apply(super.setReadOnly(name))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, times: Map[Access, Long]) =
    apply(super.setTime(options, name, times))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) =
    apply(super.setTime(options, name, types, value))

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends DecoratingInputSocket[Entry](super.input(options, name)) {
      override def localTarget() = apply(boundSocket.localTarget())
      override def stream() = apply(new SyncInputStream(boundSocket.stream()))
      override def channel() = apply(new SyncSeekableChannel(boundSocket.channel()))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Option[Entry]) = {
    final class Output extends DecoratingOutputSocket[Entry](super.output(options, name, template)) {
      override def localTarget = apply(boundSocket.localTarget)
      override def stream() = apply(new SyncOutputStream(boundSocket.stream))
      override def channel() = apply(new SyncSeekableChannel(boundSocket.channel))
    }
    new Output
  }: AnyOutputSocket

  private class SyncInputStream(in: InputStream)
  extends DecoratingInputStream(in) {
    override def close = apply(in.close())
  }

  private class SyncOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) {
    override def close = apply(out.close())
  }

  private class SyncSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) {
    override def close = apply(channel.close())
  }

  abstract override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Option[Entry]) =
    apply(super.mknod(options, name, tµpe, template))

  abstract override def unlink(options: AccessOptions, name: FsEntryName) =
    apply {
      // HC SVNT DRACONES!
      super.unlink(options, name)
      // Eventually make the file system controller chain eligible for GC.
      if (name.isRoot) super.sync(RESET)
    }

  abstract override def sync(options: SyncOptions) = safeSync(options)

  /**
   * Modifies the sync options so that no dead lock can appear due to waiting
   * for I/O resources in a recursive file system operation.
   * 
   * @param  options the sync options
   * @return the potentially modified sync options.
   */
  private def safeSync(options: SyncOptions) {
    val modified = SyncController.modify(options)
    try {
      super.sync(modified)
    } catch {
      case ex: FsSyncWarningException => throw ex
      case ex: FsSyncException =>
        if (modified ne options) {
          ex.getCause match {
            case _: FsResourceOpenException => throw NeedsLockRetryException()
            case _ =>
          }
        }
        throw ex
    }
  }

  /**
   * Applies the given file system operation and syncs the decorated controller
   * when appropriate.
   * 
   * @throws FsSyncWarningException if <em>only</em> warning conditions
   *         apply.
   *         This implies that the respective parent file system has been
   *         synchronized with constraints, e.g. if an unclosed archive entry
   *         stream gets forcibly closed.
   * @throws FsSyncException if any error conditions apply.
   */
  private def apply[V](operation: => V): V = {
    while (true) {
      try {
        return operation
      } catch {
        case opEx: NeedsSyncException =>
          checkWriteLockedByCurrentThread
          try {
            safeSync(SYNC)
          } catch {
            case syncEx: FsSyncException =>
              syncEx addSuppressed opEx
              throw syncEx
          }
      }
    }
    throw new AssertionError("dead code")
  }
}

private object SyncController {
  private val NOT_WAIT_CLOSE_IO = BitField.of(WAIT_CLOSE_IO).not

  final def modify(options: SyncOptions) =
    if (1 < LockingStrategy.lockCount) options.and(NOT_WAIT_CLOSE_IO)
    else options
}
