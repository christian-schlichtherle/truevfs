/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.truevfs.kernel.spec.util._
import net.truevfs.kernel.spec.util._

/** Indicates that a file system controller needs to get `sync`ed before the
  * operation can get retried.
  *
  * @see    FsSyncController
  * @author Christian Schlichtherle
  */
@Immutable
private final class NeedsSyncException private ()
extends ControlFlowException

private object NeedsSyncException {
  import ControlFlowException._

  private[this] val instance = new NeedsSyncException

  def apply() = if (isTraceable) new NeedsSyncException else instance
}
