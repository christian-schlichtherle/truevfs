/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec.sl._
import org.scalatest.Matchers._
import org.scalatest._

/** @author Christian Schlichtherle */
class FsManagerLocatorTest extends WordSpec {

  "The file system manager locator singleton" should {
    "provide a default file system manager" in {
      FsManagerLocator.SINGLETON.get.isInstanceOf[DefaultManager] should equal (true)
    }
  }
}
