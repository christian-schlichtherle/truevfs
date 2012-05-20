/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * A file system model which supports multiple concurrent reader threads.
 *
 * @see    LockController
 * @see    NeedsWriteLockException
 * @author Christian Schlichtherle
 */
private final class LockModel(model: FsModel)
extends FsDecoratingModel[FsModel](model) {

  /** The lock on which the file system controllers shall synchronize. */
  private val lock = new ReentrantReadWriteLock();

  def readLock = lock.readLock

  /**
   * Returns {@code true} if and only if the read lock is held by the
   * current thread.
   * This method should only get used for assert statements, not for lock
   * control!
   * 
   * @return {@code true} if and only if the read lock is held by the
   *         current thread.
   */
  def isReadLockedByCurrentThread = 0 != lock.getReadHoldCount

  def writeLock = lock.writeLock

  /**
   * Returns {@code true} if and only if the write lock is held by the
   * current thread.
   * This method should only get used for assert statements, not for lock
   * control!
   * 
   * @return {@code true} if and only if the write lock is held by the
   *         current thread.
   * @see    #checkWriteLockedByCurrentThread()
   */
  def isWriteLockedByCurrentThread = lock.isWriteLockedByCurrentThread

  /**
   * Asserts that the write lock is held by the current thread.
   * Use this method for lock control.
   * 
   * @throws NeedsWriteLockException if the <i>write lock</i> is not
   *         held by the current thread.
   * @see    #isWriteLockedByCurrentThread()
   */
  def checkWriteLockedByCurrentThread {
    if (!isWriteLockedByCurrentThread)
      throw NeedsWriteLockException.get
  }
}
