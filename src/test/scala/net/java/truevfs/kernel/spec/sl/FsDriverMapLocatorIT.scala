/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl

import org.scalatest.matchers.should.Matchers._
import org.scalatest._
import org.scalatest.wordspec.AnyWordSpec

/**
  * Tests if the class path has been properly configured so that any file system drivers are locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
class FsDriverMapLocatorIT extends AnyWordSpec {

  "The file system driver map locator singleton" should {
    "provide some file system drivers" in {
      val drivers = FsDriverMapLocator.SINGLETON.get
      drivers should not be null
      drivers.size should be > 0
    }
  }
}
