/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed

import net.java.truecommons3.shed.Filter._
import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class FilterTest extends WordSpec {

  "Filter.ACCEPT_ANY" should {
    "accept any parameter and always return true" in {
      ACCEPT_ANY.accept(null) should be (true)
    }
  }

  "Filter.ACCEPT_NONE" should {
    "accept any parameter and always return false" in {
      ACCEPT_NONE.accept(null) should be (false)
    }
  }
}
