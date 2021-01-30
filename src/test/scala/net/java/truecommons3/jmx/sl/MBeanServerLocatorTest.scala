/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.jmx.sl

import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest._

/**
  * Tests if the class path has been properly configured so that any MBean
  * server is locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class MBeanServerLocatorTest extends WordSpec with ShouldMatchers {

  "The MBean server locator singleton" should {
    "provide an MBean server" in {
      MBeanServerLocator.SINGLETON.get should not be (null)
    }
  }
}
