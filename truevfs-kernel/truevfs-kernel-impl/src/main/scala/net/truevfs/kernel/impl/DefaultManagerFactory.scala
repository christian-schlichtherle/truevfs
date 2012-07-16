/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec.spi._

/** Creates [[net.truevfs.kernel.impl.DefaultManager]] objects.
  * 
  * @author Christian Schlichtherle
  */
@Immutable
final class DefaultManagerFactory extends FsManagerFactory {
  override def apply = new DefaultManager
}
