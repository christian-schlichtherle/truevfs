/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.spi._

/**
  * @author Christian Schlichtherle
  */
@deprecated("This class is reserved for exclusive use by the [[net.java.truevfs.kernel.spec.sl.FsManagerLocator.SINGLETON]]!", "1")
final class LogManagerDecorator extends FsManagerDecorator with Immutable {

  override def apply(manager: FsManager): FsManager =
    LogMediator instrument manager

  /** Returns -100. */
  override def getPriority = -100
}
