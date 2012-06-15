/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.sl

import org.junit._
import Assert._
import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsManagerLocatorSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "A file system manager" should {
    "be locatable" in {
      FsManagerLocator.SINGLETON.getManager should not be (null)
    }
  }
}
