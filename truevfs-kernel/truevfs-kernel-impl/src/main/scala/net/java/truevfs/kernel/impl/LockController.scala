/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io._
import java.nio.channels._

import javax.annotation.concurrent._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio._
import net.java.truecommons.io._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.impl.LockingStrategy._
import net.java.truevfs.kernel.spec._

import scala.Option

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
private trait LockController[E <: FsArchiveEntry] extends ArchiveController[E] {
  controller: ArchiveModelAspect[E] =>

  abstract override def node(options: AccessOptions, name: FsNodeName): Option[FsNode] = {
    timedReadOrWriteLocked(super.node(options, name))
  }

  abstract override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]): Unit = {
    timedReadOrWriteLocked(super.checkAccess(options, name, types))
  }

  abstract override def setReadOnly(options: AccessOptions, name: FsNodeName): Unit = {
    timedLocked(writeLock)(super.setReadOnly(options, name))
  }

  abstract override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]): Boolean = {
    timedLocked(writeLock)(super.setTime(options, name, times))
  }

  abstract override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean = {
    timedLocked(writeLock)(super.setTime(options, name, types, value))
  }

  abstract override def input(options: AccessOptions, name: FsNodeName): AnyInputSocket = {
    new AbstractInputSocket[Entry] {

      private val socket = LockController.super.input(options, name)

      override def target(): Entry = fastLocked(writeLock)(socket.target())

      override def stream(peer: AnyOutputSocket): LockInputStream =
        timedLocked(writeLock)(new LockInputStream(socket stream peer))

      override def channel(peer: AnyOutputSocket): LockSeekableChannel =
        timedLocked(writeLock)(new LockSeekableChannel(socket channel peer))
    }
  }

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]): AnyOutputSocket = {
    new AbstractOutputSocket[Entry] {

      private val socket = LockController.super.output(options, name, template)

      override def target(): Entry = fastLocked(writeLock)(socket.target())

      override def stream(peer: AnyInputSocket): LockOutputStream =
        timedLocked(writeLock)(new LockOutputStream(socket stream peer))

      override def channel(peer: AnyInputSocket): LockSeekableChannel =
        timedLocked(writeLock)(new LockSeekableChannel(socket channel peer))
    }
  }

  abstract override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]): Unit = {
    timedLocked(writeLock)(super.make(options, name, tµpe, template))
  }

  abstract override def unlink(options: AccessOptions, name: FsNodeName): Unit = {
    timedLocked(writeLock)(super.unlink(options, name))
  }

  abstract override def sync(options: SyncOptions): Unit = timedLocked(writeLock)(super.sync(options))

  private def timedReadOrWriteLocked[V](operation: => V) = {
    try {
      timedLocked(readLock)(operation)
    } catch {
      case ex: NeedsWriteLockException =>
        if (readLockedByCurrentThread) {
          throw ex
        }
        timedLocked(writeLock)(operation)
    }
  }

  private class LockInputStream(in: InputStream)
  extends DecoratingInputStream(in) {

    override def close(): Unit = deadLocked(writeLock)(in.close())
  }

  private class LockOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) {

    override def close(): Unit = deadLocked(writeLock)(out.close())
  }

  private class LockSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) {

    override def close(): Unit = deadLocked(writeLock)(channel.close())
  }
}
