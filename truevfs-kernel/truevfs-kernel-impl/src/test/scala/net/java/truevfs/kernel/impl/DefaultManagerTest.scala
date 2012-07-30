/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed.Link._
import net.java.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
class DefaultManagerTest extends FsManagerTestSuite {
  override def newManager(tµpe: Type): FsManager = new DefaultManager(tµpe)
}
