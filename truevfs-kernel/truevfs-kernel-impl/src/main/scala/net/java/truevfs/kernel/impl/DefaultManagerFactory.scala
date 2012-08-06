/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.spi._

/** Creates [[net.java.truevfs.kernel.impl.DefaultManager]] objects.
  * 
  * @author Christian Schlichtherle
  */
@Immutable
final class DefaultManagerFactory extends FsManagerFactory {
  override def get: FsManager = new DefaultManager
}
