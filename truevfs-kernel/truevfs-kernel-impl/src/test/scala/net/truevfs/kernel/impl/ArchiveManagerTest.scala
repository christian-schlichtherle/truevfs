/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec.util.Link._

/**
  * @author Christian Schlichtherle
  */
class ArchiveManagerTest extends FsManagerTestSuite {
  override def newManager(tµpe: Type): FsManager = new ArchiveManager(tµpe)
}
