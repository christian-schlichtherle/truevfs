/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation.concurrent._

/** A mixin which provides some features of its associated read/write `lock`.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait ReadWriteLockAspect[+L <: ReadWriteLock] {

  /** The read/write lock with the features to provide as an aspect. */
  def lock: L

  /** Returns the read lock. */
  final def readLock = lock.readLock

  /** Runs the given operation while the read lock is held. */
  final def readLocked[V] = LockOn[V](readLock)_

  /** Returns the write lock. */
  final def writeLock = lock.writeLock

  /** Runs the given operation while the write lock is held. */
  final def writeLocked[V] = LockOn[V](writeLock)_
}
