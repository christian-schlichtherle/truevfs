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
import javax.annotation.concurrent._

/** Provides read/write locking for multi-threaded access by its clients.
  * 
  * This controller is a barrier for
  * [[de.schlichtherle.truevfs.kernel.se.NeedsWriteLockException]]s:
  * Whenever the decorated controller chain throws a `NeedsWriteLockException`,
  * the read lock gets released before the write lock gets acquired and the
  * operation gets retried.
  * 
  * This controller is also an emitter of and a barrier for
  * [[de.schlichtherle.truevfs.kernel.se.NeedsLockRetryException]]s:
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
@Immutable
private trait LockController extends Controller[LockModel] {
  this: LockModelAspect =>

  abstract override def stat(options: AccessOptions, name: FsEntryName) =
    timedReadOrWriteLocked(super.stat(options, name))

  abstract override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) =
    timedReadOrWriteLocked(super.checkAccess(options, name, types))

  abstract override def setReadOnly(name: FsEntryName) =
    timedLocked(writeLock)(super.setReadOnly(name))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, times: Map[Access, Long]) =
    timedLocked(writeLock)(super.setTime(options, name, times))

  abstract override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) =
    timedLocked(writeLock)(super.setTime(options, name, types, value))

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends  DecoratingInputSocket[Entry](super.input(options, name)) {
      override def localTarget() = fastLocked(writeLock)(boundSocket.localTarget())

      override def stream() =
        timedLocked(writeLock)(new LockInputStream(boundSocket.stream()))

      override def channel() =
        timedLocked(writeLock)(new LockSeekableChannel(boundSocket.channel()))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Option[Entry]) = {
    final class Output extends DecoratingOutputSocket[Entry](super.output(options, name, template)) {
      override def localTarget() = fastLocked(writeLock)(boundSocket.localTarget())

      override def stream() =
        timedLocked(writeLock)(new LockOutputStream(boundSocket.stream()))

      override def channel() =
        timedLocked(writeLock)(new LockSeekableChannel(boundSocket.channel()))
    }
    new Output
  }: AnyOutputSocket

  abstract override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Option[Entry]) =
    timedLocked(writeLock)(super.mknod(options, name, tµpe, template))

  abstract override def unlink(options: AccessOptions, name: FsEntryName) =
    timedLocked(writeLock)(super.unlink(options, name))

  abstract override def sync(options: SyncOptions) =
    timedLocked(writeLock)(super.sync(options))

  private def timedReadOrWriteLocked[V](operation: => V) = {
    try {
      timedLocked(readLock)(operation)
    } catch {
      case _: NeedsWriteLockException => timedLocked(writeLock)(operation)
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
