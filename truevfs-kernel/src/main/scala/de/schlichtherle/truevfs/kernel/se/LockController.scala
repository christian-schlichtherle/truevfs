/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import de.schlichtherle.truevfs.kernel.se.LockingStrategy._
import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._

/**
 * Provides read/write locking for multi-threaded access by its clients.
 * <p>
 * This controller is a barrier for {@link NeedsWriteLockException}s:
 * Whenever the decorated controller chain throws a
 * {@code NeedsWriteLockException},
 * the read lock gets released before the write lock gets acquired and the
 * operation gets retried.
 * <p>
 * This controller is also an emitter and a barrier for
 * {@link NeedsLockRetryException}s:
 * If a lock can't get immediately acquired, then a
 * {@code NeedsLockRetryException} gets thrown.
 * This will unwind the stack of federated file systems until the 
 * {@code LockController} for the first visited file system is found.
 * This controller will then pause the current thread for a small random amount
 * of milliseconds before retrying the operation.
 * 
 * @see    LockModel
 * @see    LockManagement
 * @see    NeedsWriteLockException
 * @see    NeedsLockRetryException
 * @author Christian Schlichtherle
 */
private trait LockController
extends FsController[LockModel] with LockModelController {

  override val readLock = getModel.readLock
  override val writeLock = getModel.writeLock

  abstract override def stat(options: AccessOptions, name: FsEntryName) =
    timedReadOrWriteLocked(super.stat(options, name))

  abstract override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) =
    timedReadOrWriteLocked(super.checkAccess(options, name, types))

  abstract override def setReadOnly(name: FsEntryName) =
    timedWriteLocked(super.setReadOnly(name))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, times: java.util.Map[Access, java.lang.Long]) =
    timedWriteLocked(super.setTime(options, name, times))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) =
    timedWriteLocked(super.setTime(options, name, types, value))

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends  DecoratingInputSocket[Entry](super.input(options, name)) {
      override def localTarget() = fastWriteLocked(boundSocket.localTarget())

      override def stream() =
        timedWriteLocked(new LockInputStream(boundSocket.stream()))

      override def channel() =
        timedWriteLocked(new LockSeekableChannel(boundSocket.channel()))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Entry) = {
    final class Output extends DecoratingOutputSocket[Entry](super.output(options, name, template)) {
      override def localTarget() = fastWriteLocked(boundSocket.localTarget())

      override def stream() =
        timedWriteLocked(new LockOutputStream(boundSocket.stream()))

      override def channel() =
        timedWriteLocked(new LockSeekableChannel(boundSocket.channel()))
    }
    new Output
  }: AnyOutputSocket

  private class LockInputStream(in: InputStream)
  extends DecoratingInputStream(in) {
    override def close = deadWriteLocked(in.close)
  }

  private class LockOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) {
    override def close = deadWriteLocked(out.close)
  }

  private class LockSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) {
    override def close = deadWriteLocked(channel.close)
  }

  abstract override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Entry) =
    timedWriteLocked(super.mknod(options, name, tµpe, template))

  abstract override def unlink(options: AccessOptions, name: FsEntryName) =
    timedWriteLocked(super.unlink(options, name))

  abstract override def sync(options: SyncOptions) =
    timedWriteLocked(super.sync(options))

  private def fastWriteLocked[V](operation: => V) = {
      assert(!isReadLockedByCurrentThread, "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!")
      FAST_LOCK.apply(writeLock, operation)
  }

  private def timedReadOrWriteLocked[V](operation: => V) = {
    try {
      timedReadLocked(operation);
    } catch {
      case _: NeedsWriteLockException => timedWriteLocked(operation)
    }
  }

  private def timedReadLocked[V](operation: => V) =
    TIMED_LOCK.apply(readLock, operation)

  private def timedWriteLocked[V](operation: => V) = {
    assert(!isReadLockedByCurrentThread, "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!")
    TIMED_LOCK.apply(writeLock, operation)
  }

  private def deadWriteLocked[V](operation: => V) =
    DEAD_LOCK.apply(writeLock, operation)
}
