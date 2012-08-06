/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.spi._

/** Creates a default file system manager.
  * 
  * @author Christian Schlichtherle
  */
@deprecated("This class is reserved for exclusive use by the net.java.truevfs.kernel.spec.sl.FsManagerLocator.SINGLETON")
@Immutable
final class DefaultManagerFactory extends FsManagerFactory {
  override def get: FsManager = new DefaultManager
}
