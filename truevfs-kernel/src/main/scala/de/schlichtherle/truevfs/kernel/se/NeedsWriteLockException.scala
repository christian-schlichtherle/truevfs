/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import javax.annotation.concurrent._

/** Indicates that the file system needs to get write locked before the
  * operation can get retried.
  *
  * @see    LockController
  * @author Christian Schlichtherle
  */
@Immutable
private final class NeedsWriteLockException private()
extends ControlFlowException

private object NeedsWriteLockException {
  import ControlFlowException._

  private[this] val instance = new NeedsWriteLockException

  def apply() = if (traceable) new NeedsWriteLockException else instance
}
