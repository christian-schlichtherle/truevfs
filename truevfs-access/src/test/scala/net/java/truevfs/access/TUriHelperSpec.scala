/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import java.io.File._
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
extends WordSpec
with ShouldMatchers
with PropertyChecks
with ParallelTestExecution {

  "Checking a URI" should {
    "result in a URISyntaxException" when {
      "providing a URI with a fragment component" in {
        val table = Table(
          ("uri"),
          ("#bar")
        )
        forAll(table) { _uri =>
          val uri = new URI(_uri)
          intercept[URISyntaxException] (check(uri))
        }
      }
    }
  }

  // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297 :
  "Fixing a URI" should {
    "be required unless bug 7198297 has been fixed in the JDK" in {
      new URI("x/").resolve("..").getRawSchemeSpecificPart() should be (null)
      new URI("x/").resolve("..").getSchemeSpecificPart() should be (null)
    }

    "should successfully work around bug 7198297" in {
      fix(new URI("x/").resolve("..")).getRawSchemeSpecificPart should not be (null)
      fix(new URI("x/").resolve("..")).getSchemeSpecificPart should not be (null)
    }
  }

  "Testing a URI for an absolute path" should {
    "work correctly" in {
      if ('\\' == separatorChar) {
        dinnerForOne(Table(
            ("uri", "result"),
            ("C%3A/", true),
            ("C%3A", false)
          ))
      }
      dinnerForOne(Table(
          ("uri", "result"),
          ("foo:/", true),
          ("foo:/bar", true),
          ("foo:bar", false),
          ("/", true),
          ("/foo", true),
          ("foo", false),
          ("/foo/", true),
          ("foo/", false),
          ("//host", true),
          ("//host/", true),
          ("//host/share", true)
        ))

      def dinnerForOne(table: TableFor2[String, Boolean]) {
        forAll(table) { (_uri, result) =>
          val uri = new URI(_uri)
          hasAbsolutePath(uri) should be (result)
        }
      }
    }
  }
}
