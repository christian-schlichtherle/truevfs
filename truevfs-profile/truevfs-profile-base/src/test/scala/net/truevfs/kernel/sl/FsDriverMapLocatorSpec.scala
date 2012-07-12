/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.sl

import net.truevfs.kernel.spec.sl._
import org.junit._
import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._

/**
  * Tests if the class path has been properly configured so that any file
  * system drivers are locatable at RUNTIME!
  * 
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsDriverMapLocatorSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "The file system driver map locator singleton" should {
    "provide some file system drivers" in {
      val drivers = FsDriverMapLocator.SINGLETON.drivers
      drivers should not be (null)
      drivers.size should be > (0)
    }
  }
}
