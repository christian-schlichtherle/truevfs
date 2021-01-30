/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import net.java.truevfs.access.TUriHelper._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.wordspec.AnyWordSpec

import java.io.File._
import java.net._

/**
 * @author Christian Schlichtherle
 */
class TUriHelperSpec extends AnyWordSpec {

  "Checking a URI" should {
    "result in a URISyntaxException" when {
      "providing a URI with a fragment component" in {
        val table = Table(
          "uri",
          "#bar"
        )
        forAll(table) { _uri =>
          val uri = new URI(_uri)
          intercept[URISyntaxException](check(uri))
        }
      }
    }
  }

  // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297 :
  "Fixing a URI" should {
    "successfully work around bug 7198297" in {
      fix(new URI("x/") resolve "..").getRawSchemeSpecificPart shouldBe ""
      fix(new URI("x/") resolve "..").getSchemeSpecificPart shouldBe ""
    }
  }

  "Testing a URI for an absolute path" should {
    "work correctly" in {
      if ('\\' == separatorChar) {
        assertHasAbsolutePath(Table(
          ("uri", "result"),
          ("C%3A/", true),
          ("C%3A", false)
        ))
      }
      assertHasAbsolutePath(Table(
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

      def assertHasAbsolutePath(table: TableFor2[String, Boolean]): Unit = {
        forAll(table) { (uri, result) =>
          hasAbsolutePath(new URI(uri)) shouldBe result
        }
      }
    }
  }
}

object TUriHelperSpec {

  private val VersionRegex = """(\d+\.\d+).*""".r.anchored
}