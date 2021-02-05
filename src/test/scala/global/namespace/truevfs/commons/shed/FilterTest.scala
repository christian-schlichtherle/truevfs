/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.shed

import global.namespace.truevfs.commons.shed.Filter._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class FilterTest extends AnyWordSpec {

  "Filter.ACCEPT_ANY" should {
    "accept any parameter and always return true" in {
      ACCEPT_ANY.accept(null) should be(true)
    }
  }

  "Filter.ACCEPT_NONE" should {
    "accept any parameter and always return false" in {
      ACCEPT_NONE.accept(null) should be(false)
    }
  }
}
