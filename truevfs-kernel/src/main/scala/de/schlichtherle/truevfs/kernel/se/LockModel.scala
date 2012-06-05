/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.truevfs.kernel._

/** A file system model which supports multiple concurrent reader threads.
  *
  * @see    LockController
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class LockModel(model: FsModel)
extends FsDecoratingModel[FsModel](model) {

  /** The lock on which the file system controller(s) shall synchronize. */
  private val lock = new ReentrantReadWriteLock

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
  def readLockedByCurrentThread = 0 != lock.getReadHoldCount

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
  def writeLockedByCurrentThread = lock.isWriteLockedByCurrentThread
}
