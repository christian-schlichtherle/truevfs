/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks.ReentrantReadWriteLock._
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent._

/** A mixin which provides some features of its associated `lock`.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait ReentrantReadWriteLockAspect {

  /** The lock with the features to provide as an aspect. */
  def lock: ReentrantReadWriteLock

  /** Returns the read lock. */
  final def readLock = lock.readLock

  final def readLocked[V] = lockOn[V](readLock)_
  
  /** Returns `true` if and only if the read lock is held by the
    * current thread.
    * This method should only get used for assertions, not for lock control.
    * 
    * @return `true` if and only if the read lock is held by the
    *         current thread.
    */
  final def readLockedByCurrentThread = 0 != lock.getReadHoldCount

  /** Returns the write lock. */
  final def writeLock = lock.writeLock

  final def writeLocked[V] = lockOn[V](writeLock)_

  /** Returns `true` if and only if the write lock is held by the current
    * thread.
    * This method should only get used for assertions, not for lock control.
    * 
    * @return `true` if and only if the write lock is held by the current
    *         thread.
    */
   final def writeLockedByCurrentThread = lock.isWriteLockedByCurrentThread

  /** Checks that the write lock is held by the current thread.
    * Use this method for lock control.
    * 
    * @throws NeedsWriteLockException if the `writeLock` is not held by the
    *         current thread.
    * @see    #writeLockedByCurrentThread
    */
  final def checkWriteLockedByCurrentThread() {
    if (!writeLockedByCurrentThread) throw NeedsWriteLockException()
  }
}
