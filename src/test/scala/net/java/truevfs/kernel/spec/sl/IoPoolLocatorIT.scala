/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl

import org.scalatest.Matchers._
import org.scalatest._

/**
  * Tests if the class path has been properly configured so that any I/O buffer
  * pool is locatable at RUNTIME!
  *
  * @author Christian Schlichtherle
  */
class IoPoolLocatorIT extends WordSpec {

  "The I/O buffer pool locator singleton" should {
    "provide an I/O buffer pool" in {
      (IoBufferPoolLocator.SINGLETON.get: AnyRef) should not be null
    }
  }
}
