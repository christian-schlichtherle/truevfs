/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec.sl._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.Matchers._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class FsManagerLocatorSpec extends WordSpec {

  "The file system manager locator singleton" should {
    "provide a default file system manager" in {
      FsManagerLocator.SINGLETON.get.isInstanceOf[DefaultManager] should equal (true)
    }
  }
}
