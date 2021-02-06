/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx.sl

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
 * Tests if the class path has been properly configured so that any MBean
 * server is locatable at RUNTIME!
 *
 * @author Christian Schlichtherle
 */
class MBeanServerLocatorSpec extends AnyWordSpec {

  "The MBean server locator singleton" should {
    "provide an MBean server" in {
      MBeanServerLocator.SINGLETON.get shouldNot be(null)
    }
  }
}
