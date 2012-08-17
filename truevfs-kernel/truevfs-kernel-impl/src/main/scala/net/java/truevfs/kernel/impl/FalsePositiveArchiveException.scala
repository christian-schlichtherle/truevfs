/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import java.io._

/** Indicates that a file system is a false positive file system.
  * 
  * This exception type is reserved for non-local control flow in
  * file system controller chains in order to reroute file system operations to
  * the parent file system of a false positive federated (archive) file system.
  *
  * @see    FalsePositiveArchiveController
  * @author Christian Schlichtherle
  */
private class FalsePositiveArchiveException(cause: IOException)
extends ControlFlowException(cause) with Immutable {
  override def getCause() = super.getCause.asInstanceOf[IOException]
}
