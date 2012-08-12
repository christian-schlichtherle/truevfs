/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsSyncOption._
import net.java.truevfs.kernel.spec.FsSyncOptions._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._

/** Performs a `sync` operation if required.
  * 
  * This controller is a barrier for
  * [[net.java.truevfs.kernel.impl.NeedsSyncException]]s:
  * Whenever the decorated controller chain throws a `NeedsSyncException`,
  * the file system gets `sync`ed before the operation gets retried.
  * 
  * @see    NeedsSyncException
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait SyncController extends Controller[LockModel] {
  this: LockModelAspect =>

  abstract override def node(options: AccessOptions, name: FsNodeName) =
    apply(super.node(options, name))

  abstract override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    apply(super.checkAccess(options, name, types))

  abstract override def setReadOnly(name: FsNodeName) =
    apply(super.setReadOnly(name))

  abstract override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]) =
    apply(super.setTime(options, name, times))

  abstract override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    apply(super.setTime(options, name, types, value))

  abstract override def input(options: AccessOptions, name: FsNodeName) = {
    final class Input extends AbstractInputSocket[Entry] {
      private[this] val socket = SyncController.super.input(options, name)

      override def target() = apply(socket.target())

      override def stream(peer: AnyOutputSocket) =
        apply(new SyncInputStream(socket stream peer))

      override def channel(peer: AnyOutputSocket) =
        apply(new SyncSeekableChannel(socket channel peer))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]) = {
    final class Output extends AbstractOutputSocket[Entry] {
      private[this] val socket = SyncController.super.output(options, name, template)

      override def target = apply(socket.target)

      override def stream(peer: AnyInputSocket) =
        apply(new SyncOutputStream(socket stream peer))

      override def channel(peer: AnyInputSocket) =
        apply(new SyncSeekableChannel(socket channel peer))
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

  abstract override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) =
    apply(super.make(options, name, tµpe, template))

  abstract override def unlink(options: AccessOptions, name: FsNodeName) =
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
      case ex: FsSyncWarningException => throw ex // may be FORCE_CLOSE_IO was set, too?
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
