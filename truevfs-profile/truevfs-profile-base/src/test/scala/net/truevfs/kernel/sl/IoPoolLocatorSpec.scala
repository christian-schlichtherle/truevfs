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
  * Tests if the class path has been properly configured so that any I/O buffer
  * pool is locatable at RUNTIME!
  * 
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class IoPoolLocatorSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "The I/O buffer pool locator singleton" should {
    "provide an I/O buffer pool" in {
      (IoBufferPoolLocator.SINGLETON.get: AnyRef) should not be (null)
    }
  }
}
