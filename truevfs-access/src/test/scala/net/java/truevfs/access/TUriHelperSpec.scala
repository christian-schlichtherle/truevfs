/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import java.net._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.mock._
import org.scalatest.prop._
import TUriHelper._

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class TUriHelperSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297 :
  "fixing a URI" should {
    "be required unless bug 7198297 has been fixed in the JDK" in {
      new URI("x/").resolve("..").getRawSchemeSpecificPart() should be (null)
      new URI("x/").resolve("..").getSchemeSpecificPart() should be (null)
    }

    "should successfully work around bug 7198297" in {
      fix(new URI("x/").resolve("..")).getRawSchemeSpecificPart should not be (null)
      fix(new URI("x/").resolve("..")).getSchemeSpecificPart should not be (null)
    }
  }
}
