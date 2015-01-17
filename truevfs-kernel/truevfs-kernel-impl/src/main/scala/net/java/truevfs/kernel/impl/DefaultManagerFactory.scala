/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.spi._

/** Creates a default file system manager.
  *
  * @author Christian Schlichtherle
  */
final class DefaultManagerFactory
extends FsManagerFactory with Immutable {

  override def get: FsManager = new DefaultManager

  /** @return -100 */
  override def getPriority = -100
}
