/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.spi._

/**
 * @author Christian Schlichtherle
 */
@deprecated("This class is reserved for exclusive use by the [[net.java.truevfs.kernel.spec.sl.FsManagerLocator.SINGLETON]]!", "1")
final class I5tManagerDecorator extends FsManagerDecorator {
  def apply(manager: FsManager) = I5tMediator.get.instrument(manager)
}
