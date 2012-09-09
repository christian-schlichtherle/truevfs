/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._;
import net.java.truevfs.kernel.impl.LockingStrategy._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._

/** Provides read/write locking for multi-threaded access by its clients.
  * 
  * This controller is a barrier for
  * [[net.java.truevfs.kernel.impl.NeedsWriteLockException]]s:
  * Whenever the decorated controller chain throws a `NeedsWriteLockException`,
  * the read lock gets released before the write lock gets acquired and the
  * operation gets retried.
  * 
  * This controller is also an emitter of and a barrier for
  * [[net.java.truevfs.kernel.impl.NeedsLockRetryException]]s:
  * If a lock can't get immediately acquired, then a `NeedsLockRetryException`
  * gets thrown.
  * This will unwind the stack of federated file systems until the
  * `LockController` for the first visited file system is found.
  * This controller will then pause the current thread for a small random
  * amount of milliseconds before retrying the operation.
  * 
  * @see    LockingStrategy
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockController[E <: FsArchiveEntry]
extends ArchiveController[E] {

  abstract override def node(options: AccessOptions, name: FsNodeName) =
    timedReadOrWriteLocked(super.node(options, name))

  abstract override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    timedReadOrWriteLocked(super.checkAccess(options, name, types))

  abstract override def setReadOnly(name: FsNodeName) =
    timedLocked(writeLock)(super.setReadOnly(name))

  abstract override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]) =
    timedLocked(writeLock)(super.setTime(options, name, times))

  abstract override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    timedLocked(writeLock)(super.setTime(options, name, types, value))

  abstract override def input(options: AccessOptions, name: FsNodeName) = {
    final class Input extends AbstractInputSocket[Entry] {
      private[this] val socket = LockController.super.input(options, name)

      override def target() = fastLocked(writeLock)(socket.target())

      override def stream(peer: AnyOutputSocket) =
        timedLocked(writeLock)(new LockInputStream(socket.stream(peer)))

      override def channel(peer: AnyOutputSocket) =
        timedLocked(writeLock)(new LockSeekableChannel(socket.channel(peer)))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]) = {
    final class Output extends AbstractOutputSocket[Entry] {
      private[this] val socket = LockController.super.output(options, name, template)

      override def target() = fastLocked(writeLock)(socket.target())

      override def stream(peer: AnyInputSocket) =
        timedLocked(writeLock)(new LockOutputStream(socket.stream(peer)))

      override def channel(peer: AnyInputSocket) =
        timedLocked(writeLock)(new LockSeekableChannel(socket.channel(peer)))
    }
    new Output
  }: AnyOutputSocket

  abstract override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) =
    timedLocked(writeLock)(super.make(options, name, tµpe, template))

  abstract override def unlink(options: AccessOptions, name: FsNodeName) =
    timedLocked(writeLock)(super.unlink(options, name))

  abstract override def sync(options: SyncOptions) =
    timedLocked(writeLock)(super.sync(options))

  private def timedReadOrWriteLocked[V](operation: => V) = {
    try {
      timedLocked(readLock)(operation)
    } catch {
      case ex: NeedsWriteLockException =>
        if (readLockedByCurrentThread) throw ex
        timedLocked(writeLock)(operation)
    }
  }

  private class LockInputStream(in: InputStream)
  extends DecoratingInputStream(in) {
    override def close = deadLocked(writeLock)(in.close)
  }

  private class LockOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) {
    override def close = deadLocked(writeLock)(out.close)
  }

  private class LockSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) {
    override def close = deadLocked(writeLock)(channel.close)
  }
}
