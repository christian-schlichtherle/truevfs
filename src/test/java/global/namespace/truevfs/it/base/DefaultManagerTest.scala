/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.base

import global.namespace.truevfs.kernel.api._
import global.namespace.truevfs.kernel.impl.DefaultManagerFactory

/**
 * @author Christian Schlichtherle
 */
final class DefaultManagerTest extends FsManagerTestSuite {

  override def newManager(): FsManager = new DefaultManagerFactory().get()
}
