/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import javax.annotation.concurrent._

/** Indicates that an operation needs to get write locked before it can get
  * retried.
  *
  * @author Christian Schlichtherle
  */
@Immutable
private final class NeedsWriteLockException private ()
extends ControlFlowException(false)

private object NeedsWriteLockException {
  import ControlFlowException._

  private[this] val instance = new NeedsWriteLockException

  def apply() = if (isTraceable) new NeedsWriteLockException else instance
}
