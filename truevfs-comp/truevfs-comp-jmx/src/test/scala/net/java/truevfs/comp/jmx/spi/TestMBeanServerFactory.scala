/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx.spi

import javax.management.MBeanServerFactory

/** @author Christian Schlichtherle */
class TestMBeanServerFactory extends MBeanServerProvider {
  override def get = MBeanServerFactory.newMBeanServer
}
