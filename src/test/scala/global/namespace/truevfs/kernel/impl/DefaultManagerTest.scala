/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl

import global.namespace.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
class DefaultManagerTest extends FsManagerTestSuite {
  override def newManager(): FsManager = new DefaultManager
}
