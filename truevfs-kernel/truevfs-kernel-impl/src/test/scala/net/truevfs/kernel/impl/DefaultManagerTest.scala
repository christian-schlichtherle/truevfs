/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import de.schlichtherle.truecommons.shed.Link._
import net.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
class DefaultManagerTest extends FsManagerTestSuite {
  override def newManager(tµpe: Type): FsManager = new DefaultManager(tµpe)
}
