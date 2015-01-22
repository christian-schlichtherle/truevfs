/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation.concurrent._

/** A mixin which provides some features of its associated reentrant `lock`.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockAspect[+L <: Lock] {

  /** The lock with the features to provide as an aspect. */
  def lock: L

  /** Runs the given operation while the lock is held. */
  final def locked[V] = LockOn[V](lock)_
}
