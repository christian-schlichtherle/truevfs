/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
trait DriverModel[E <: FsArchiveEntry] extends GenModel {
  def driver: FsArchiveDriver[E]
  def touch(options: AccessOptions)
}
