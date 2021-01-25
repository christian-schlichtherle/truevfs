/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl

import org.scalatest.Matchers._
import org.scalatest._

/**
  * Tests if the class path has been properly configured so that any file
  * system manager is locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
class FsManagerLocatorIT extends WordSpec {

  "The file system manager locator singleton" should {
    "provide a file system manager" in {
      FsManagerLocator.SINGLETON.get should not be null
    }
  }
}
