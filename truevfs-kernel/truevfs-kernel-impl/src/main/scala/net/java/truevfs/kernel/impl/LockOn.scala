/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._

/** Runs an operation while a lock is held.
  * 
  * @author Christian Schlichtherle
  */
private object LockOn {

  /** Runs the given `operation` while the given `lock` is held.
    *
    * @param lock the lock to acquire.
    * @param operation the operation to run.
    */
  def apply[V](lock: Lock)(operation: => V): V = {
    lock.lock()
    try {
      operation
    } finally {
      lock.unlock()
    }
  }
}
