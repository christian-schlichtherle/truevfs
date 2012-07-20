/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation._
import javax.annotation.concurrent._
import net.truevfs.kernel.spec.cio._

/**
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockedOperation {

  protected val lock = new ReentrantLock();

  def locked[V](operation: => V) = {
    lock lock ()
    try {
      operation
    } finally {
      lock unlock ()
    }
  }
}
