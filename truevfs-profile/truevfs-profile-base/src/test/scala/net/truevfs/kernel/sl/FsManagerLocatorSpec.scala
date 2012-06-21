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
  * system manager is locatable at RUNTIME!
  * 
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsManagerLocatorSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "The file system manager locator singleton" should {
    "provide a file system manager" in {
      FsManagerLocator.SINGLETON.getManager should not be (null)
    }
  }
}
