/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation.concurrent._

/** A mixin which provides some features of its associated reentrant read/write
  * `lock`.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait ReentrantReadWriteLockAspect
extends ReadWriteLockAspect[ReentrantReadWriteLock] {

  /** Returns `true` if and only if the read lock is held by the
    * current thread.
    * This method should only get used for assertions, not for lock control.
    * 
    * @return `true` if and only if the read lock is held by the
    *         current thread.
    */
  final def readLockedByCurrentThread: Boolean = 0 != lock.getReadHoldCount

  /** Returns `true` if and only if the write lock is held by the current
    * thread.
    * This method should only get used for assertions, not for lock control.
    * 
    * @return `true` if and only if the write lock is held by the current
    *         thread.
    */
   final def writeLockedByCurrentThread: Boolean = lock.isWriteLockedByCurrentThread

  /** Checks that the write lock is held by the current thread.
    * Use this method for lock control.
    * 
    * @throws NeedsWriteLockException if the `writeLock` is not held by the
    *         current thread.
    * @see    #writeLockedByCurrentThread
    */
  final def checkWriteLockedByCurrentThread(): Unit = {
    if (!writeLockedByCurrentThread) throw NeedsWriteLockException()
  }
}
