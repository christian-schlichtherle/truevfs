/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import javax.annotation.concurrent._
import net.truevfs.kernel._

/** A mixin which provides some features of its associated
  * [[de.schlichtherle.truevfs.kernel.se.LockModel]].
  *
  * @see    LockModel
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockModelFeatures {

  /** The lock model with the features to provide in this trait. */
  def model: LockModel

  /** Returns the mount point of this (federated virtual) file system as
    * defined by the `model`.
    * 
    * @return The mount point of this (federated virtual) file system as
    *         defined by the `model`.
    */
  final def mountPoint = model getMountPoint

  /** Returns the `touched` property of the `model`.
    * 
    * @return The `touched` property of the `model`.
    */
  final def touched = model isTouched

  /**
   * Sets the {@code touched} property of the
   * {@linkplain #getModel() file system model}.
   * 
   * @param touched the {@code touched} property of the
   *         {@linkplain #getModel() file system model}.
   */
  final def touched_=(touched: Boolean) = model setTouched touched

  /** The read lock of the lock model. */
  final val readLock = model readLock

  /** Wether or not the current thread has acquired the read lock. */
  final def readLockedByCurrentThread = model readLockedByCurrentThread

  /** The write lock of the lock model. */
  final val writeLock = model writeLock

  /** Wether or not the current thread has acquired the write lock. */
  final def writeLockedByCurrentThread = model writeLockedByCurrentThread

  /**
   * Checks that the write lock is held by the current thread.
   * Use this method for lock control.
   * 
   * @throws NeedsWriteLockException if the <i>write lock</i> is not
   *         held by the current thread.
   * @see    #writeLockedByCurrentThread
   */
  final def checkWriteLockedByCurrentThread() {
    if (!writeLockedByCurrentThread) throw NeedsWriteLockException()
  }
}
