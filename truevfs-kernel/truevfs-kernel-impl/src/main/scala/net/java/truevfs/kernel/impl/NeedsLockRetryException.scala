/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._

/** Indicates that all file system locks need to get released before the
  * operation can get retried.
  *
  * @see    LockController
  * @author Christian Schlichtherle
  */
private final class NeedsLockRetryException private ()
extends ControlFlowException(false) with Immutable

private object NeedsLockRetryException {
  import ControlFlowException._

  private[this] val instance = new NeedsLockRetryException

  def apply() = if (isTraceable) new NeedsLockRetryException else instance
}
