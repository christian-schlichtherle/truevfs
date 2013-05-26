/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl

import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest._

/**
  * Tests if the class path has been properly configured so that any file
  * system drivers are locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsDriverMapLocatorIT extends WordSpec {

  "The file system driver map locator singleton" should {
    "provide some file system drivers" in {
      val drivers = FsDriverMapLocator.SINGLETON.get
      drivers should not be (null)
      drivers.size should be > (0)
    }
  }
}
