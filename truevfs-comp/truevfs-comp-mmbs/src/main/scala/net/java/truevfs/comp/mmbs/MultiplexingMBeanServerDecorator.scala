/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.mmbs

import javax.management._
import net.java.truevfs.comp.jmx.spi._

@deprecated("This class is reserved for exclusive use by the [[net.java.truevfs.comp.jmx.sl.MBeanServerLocator.SINGLETON]]!", "1")
final class MultiplexingMBeanServerDecorator
extends MBeanServerDecorator with Immutable {

  override def apply(mbs: MBeanServer): MBeanServer =
    MultiplexingMBeanServer(mbs)

  override def getPriority = -100
}
