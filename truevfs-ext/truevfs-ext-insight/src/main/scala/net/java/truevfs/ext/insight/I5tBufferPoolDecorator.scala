/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import net.java.truecommons.cio._
import net.java.truevfs.kernel.spec.spi._

/**
  * @author Christian Schlichtherle
  */
@deprecated("This class is reserved for exclusive use by the [[net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator.SINGLETON]]!", "1")
final class I5tBufferPoolDecorator extends IoBufferPoolDecorator {

  override def apply(pool: IoBufferPool): IoBufferPool =
    syncOperationsMediator.instrument(pool)
}
