/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class DefaultManagerFactorySpec extends WordSpec {

  "A default file system manager factory" should {
    val service = new DefaultManagerFactory

    "provide a default file system manager" in {
      service.get should not be null
    }

    "have negative priority" in {
      service.getPriority should be < 0
    }
  }
}
