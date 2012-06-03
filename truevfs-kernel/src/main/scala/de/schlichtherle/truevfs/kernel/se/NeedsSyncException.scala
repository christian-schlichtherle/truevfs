/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel.util._

/**
 * Indicates that a file system controller needs to get
 * {@linkplain Controller#sync(BitField) synced} before the operation can
 * get retried.
 *
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
private final class NeedsSyncException private()
extends ControlFlowException

private object NeedsSyncException {
  import ControlFlowException._

  private[this] val instance = new NeedsSyncException

  def apply() = if (traceable) new NeedsSyncException else instance
}
