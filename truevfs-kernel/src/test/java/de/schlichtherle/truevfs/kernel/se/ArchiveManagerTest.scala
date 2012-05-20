/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._
import net.truevfs.kernel.util.Link._

/**
 * @author Christian Schlichtherle
 */
class ArchiveManagerTest extends FsManagerTestSuite {
  override def newManager(tµpe: Type): FsManager = new ArchiveManager(tµpe)
}
