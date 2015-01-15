/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl

import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/**
  * Tests if the class path has been properly configured so that any file
  * system manager is locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsManagerLocatorIT extends WordSpec {

  "The file system manager locator singleton" should {
    "provide a file system manager" in {
      FsManagerLocator.SINGLETON.get should not be null
    }
  }
}
