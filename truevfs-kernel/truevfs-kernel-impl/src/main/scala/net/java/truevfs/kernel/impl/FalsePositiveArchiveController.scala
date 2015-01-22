/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import java.io._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec.FsNodeName._;
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._;

/** Implements a chain of responsibility for resolving
  * [[net.java.truevfs.kernel.impl.FalsePositiveArchiveException]]s which
  * may get thrown by its decorated file system controller.
  *
  * This controller is a barrier for
  * [[net.java.truevfs.kernel.impl.FalsePositiveArchiveException]]s:
  * Whenever the decorated controller chain throws a
  * `FalsePositiveArchiveException`, the file system operation is routed to the
  * controller of the parent file system in order to continue the operation.
  * If this fails with an [[java.io.IOException]], then the `IOException` which
  * is associated as the original cause of the initial
  * `FalsePositiveArchiveException` gets rethrown.
  *
  * This algorithm effectively achieves the following objectives:
  *
  * 1. False positive archive files get resolved correctly by accessing them as
  *    entities of the parent file system.
  * 2. If the file system driver for the parent file system throws another
  *    exception, then it gets discarded and the exception initially thrown by
  *    the file system driver for the false positive archive file takes its
  *    place in order to provide the caller with a good indication of what went
  *    wrong in the first place.
  * 3. Non-`IOException`s are excempt from this masquerade in order to
  *    support resolving them by a more competent caller.
  *    This is required to make
  *    [[net.java.truevfs.kernel.impl.ControlFlowException]]s work as
  *    designed.
  *
  * As an example consider accessing a RAES encrypted ZIP file:
  * With the default driver configuration of the module TrueVFS ZIP.RAES,
  * whenever a ZIP.RAES file gets mounted, the user is prompted for a password.
  * If the user cancels the password prompting dialog, then an appropriate
  * exception gets thrown.
  * The target archive controller would then catch this exception and flag the
  * archive file as a false positive by wrapping this exception in a
  * `FalsePositiveArchiveException`.
  * This class would then catch this false positive exception and try to
  * resolve the issue by using the parent file system controller.
  * Failing that, the initial exception would get rethrown in order to signal
  * to the caller that the user had cancelled password prompting.
  *
  * @see    FalsePositiveArchiveException
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class FalsePositiveArchiveController(
  controller: FsController
) extends FsDecoratingController(controller) {

  @volatile private[this] var state: State = TryChild

  override lazy val getParent = controller.getParent

  private lazy val path = getMountPoint.getPath

  override def node(options: AccessOptions, name: FsNodeName) =
    apply(name, (c, n) => c node (options, n))

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    apply(name, (c, n) => c checkAccess (options, n, types))

  override def setReadOnly(options: AccessOptions, name: FsNodeName) =
    apply(name, (c, n) => c setReadOnly (options, n))

  override def setTime(options: AccessOptions, name: FsNodeName, times: java.util.Map[Access, java.lang.Long]) =
    apply(name, (c, n) => c setTime (options, n, times))

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    apply(name, (c, n) => c setTime (options, n, types, value))

  override def input(options: AccessOptions, name: FsNodeName) = {
    final class Input extends AbstractInputSocket[Entry] {
      var _last: FsController = _
      var _socket: AnyInputSocket = _

      def socket(c: FsController, n: FsNodeName) = {
        if (_last ne c) { _last = c; _socket = c input (options, n) }
        _socket
      }

      override def target() = apply(name, (c, n) => socket(c, n) target ())

      override def stream(peer: AnyOutputSocket) =
        apply(name, (c, n) => socket(c, n) stream (peer))

      override def channel(peer: AnyOutputSocket) =
        apply(name, (c, n) => socket(c, n) channel (peer))
    }
    new Input
  }: AnyInputSocket

  override def output(options: AccessOptions, name: FsNodeName, template: Entry) = {
    final class Output extends AbstractOutputSocket[Entry] {
      var _last: FsController = _
      var _socket: AnyOutputSocket = _

      def socket(c: FsController, n: FsNodeName) = {
        if (_last ne c) { _last = c; _socket = c output (options, n, template) }
        _socket
      }

      override def target() = apply(name, (c, n) => socket(c, n) target ())

      override def stream(peer: AnyInputSocket) =
        apply(name, (c, n) => socket(c, n) stream (peer))

      override def channel(peer: AnyInputSocket) =
        apply(name, (c, n) => socket(c, n) channel (peer))
    }
    new Output
  }: AnyOutputSocket

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Entry) =
    apply(name, (c, n) => c make (options, n, tµpe, template))

  override def unlink(options: AccessOptions, name: FsNodeName) {
    val operation: Operation[Unit] = { (c, n) =>
      c unlink (options, n)
      if (n.isRoot) {
        assert (c eq controller)
        // Unlink target archive file from parent file system.
        // This operation isn't lock protected, so it's not atomic!
        getParent unlink (options, parent(n))
      }
    }
    if (name.isRoot) {
      // HC SVNT DRACONES!
      try {
        TryChild(ROOT, operation)
      } catch {
        case ex: FalsePositiveArchiveException =>
          UseParent(ex)(ROOT, operation)
      }
      this.state = TryChild
    } else {
      apply(name, operation)
    }
  }

  override def sync(options: SyncOptions) {
    // HC SVNT DRACONES!
    try {
      controller sync (options)
    } catch {
      case ex: FsSyncException =>
        assert(state eq TryChild)
        throw ex
      case ex: ControlFlowException =>
        assert(state eq TryChild)
        throw ex
    }
    state = TryChild
  }

  private def apply[V](name: FsNodeName, operation: Operation[V]): V = {
    var state = this.state
    try {
      state(name, operation)
    } catch {
      case ex: PersistentFalsePositiveArchiveException =>
        assert(state eq TryChild)
        state = UseParent(ex)
        this.state = state
        state(name, operation)
      case ex: FalsePositiveArchiveException =>
        assert(state eq TryChild)
        UseParent(ex)(name, operation)
    }
  }

  private def parent(name: FsNodeName) = path.resolve(name).getNodeName

  private type Operation[V] = (FsController, FsNodeName) => V

  private sealed trait State {
    def apply[V](name: FsNodeName, operation: Operation[V]): V
  } // State

  private object TryChild extends State {
    override def apply[V](name: FsNodeName, operation: Operation[V]) =
      operation(controller, name)
  } // TryChild

  private case class UseParent(original: FalsePositiveArchiveException)
  extends State {
    val originalCause = original.getCause

    override def apply[V](name: FsNodeName, operation: Operation[V]) = {
      try {
        operation(getParent, parent(name))
      } catch {
        case caught: FalsePositiveArchiveException =>
          throw new AssertionError(caught)
        case caught: IOException =>
          if (originalCause ne caught) originalCause addSuppressed caught
          throw originalCause
        case caught: Throwable =>
          assert(!caught.isInstanceOf[ControlFlowException]
                 || caught.isInstanceOf[NeedsLockRetryException])
          caught addSuppressed original // provide full context
          throw caught
      }
    }
  } // UseParent
}
