/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec.cio._

/**
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockAspect {

  def lock: Lock

  final def locked[V] = LockOn[V](lock)_
}
