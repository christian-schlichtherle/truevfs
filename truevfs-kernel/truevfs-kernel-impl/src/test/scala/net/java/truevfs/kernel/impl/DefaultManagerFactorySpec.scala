/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import org.junit._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class DefaultManagerFactorySpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "A default file system manager factory" should {
    val service = new DefaultManagerFactory

    "provide a default file system manager" in {
      service.get should not be (null)
    }

    "have priority 0" in {
      service.getPriority should be (0)
    }
  }
}
