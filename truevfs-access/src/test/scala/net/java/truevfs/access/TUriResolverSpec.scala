/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import java.io.File._
import java.net._

import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.mock.MockitoSugar.mock
import org.scalatest.prop.PropertyChecks._
import org.scalatest.prop.TableFor3

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class TUriResolverSpec extends WordSpec {

  "TUriResolver.parent(FsNodePath)" should {
    "return the correct parent file system node path" in {
      val table = Table(
        ("nodePath", "parentNodePath"),

        ("", null),
        ("foo", ""),
        ("file:/", null),
        ("file:/foo", "file:/"),
        ("file:/foo/", "file:/"),
        ("file:/foo/bar", "file:/foo/"),
        ("file:/foo/bar/", "file:/foo/"),
        ("mok:file:/foo!/", "file:/"),
        ("mok:file:/foo!/bar", "mok:file:/foo!/"),
        ("mok:mok:file:/foo!/bar!/", "mok:file:/foo!/"),
        ("mok:mok:file:/foo!/bar!/baz", "mok:mok:file:/foo!/bar!/"),
        ("mok:mok:file:/foo!/bar!/baz/boom", "mok:mok:file:/foo!/bar!/baz")
      )
      forAll(table) { (_nodePath, _parentNodePath) =>
        val nodePath = new FsNodePath(new URI(_nodePath))
        val parentNodePath = Option(_parentNodePath)
          .map(s => new FsNodePath(new URI(s)))
        Option(TUriResolver parent nodePath) should equal (parentNodePath)
      }
    }
  }

  "A TUriResolver" should {
    "fail with an IllegalArgumentException when resolving illegal parameters" in {
      val table = Table(
        ("base", "uri"),

        (null, "foo"),

        ("file:/", null),
        ("file:/", "bar#baz"),
        ("file:/", "..")
      )
      apply { resolver =>
        forAll(table) { (_base, _uri) =>
            val base = Option(_base) map (s => new FsNodePath(new URI(s)))
            val uri = Option(_uri) map (s => new URI(s))
            if (base.isEmpty || uri.isEmpty)
              intercept[NullPointerException] (resolver.resolve(base.orNull, uri.orNull))
            else
              intercept[IllegalArgumentException] (resolver.resolve(base.get, uri.get))
        }
      }
    }

    "correctly resolve legal parameters" in {
      if ('\\' == separatorChar) {
        test(Table(
            ("base", "uri", "resolved"),

            ( "file:/", "//foo/bar", "file://foo/bar"),
            ( "file:/", "//foo/bar/", "file://foo/bar/"),
            ( "file:/", "c%3A/foo", "file:/c:/foo"),
            ( "file:/", "c%3A/foo", "file:/c:/foo"),
            ( "file:///c:/", "//foo/bar", "file://foo/bar"),
            ( "file:///c:/", "//foo/bar/", "file://foo/bar/"),
            ( "file:///c:/", "c%3A/foo", "file:/c:/foo"),
            ( "file:///c:/", "c%3A/foo", "file:/c:/foo"),
            ( "file://host/share/", "//foo/bar", "file://foo/bar"),
            ( "file://host/share/", "//foo/bar/", "file://foo/bar/"),
            ( "file://host/share/", "c%3A/foo", "file:/c:/foo"),
            ( "file://host/share/", "c%3A/foo", "file:/c:/foo")
          ))
      }
      test(Table(
          ("base", "uri", "resolved"),

          ("foo", "bar", "foo/bar"),
          ("foo", "..", ""),
          ("foo/bar", "../..", ""),
          ("scheme:/foo", "..", "scheme:/"),
          ("scheme:/foo/bar", "", "scheme:/foo/bar"),
          ("scheme:/foo/bar", "..", "scheme:/foo/"),
          ("scheme:/foo/bar", "../..", "scheme:/"),
          ("scheme:/foo.mok/bar.mok", "../..", "scheme:/"),
          ("mok:mok:scheme:/foo.mok!/bar.mok!/", "", "mok:mok:scheme:/foo.mok!/bar.mok!/"),
          ("mok:mok:scheme:/foo.mok!/bar.mok!/", "..", "mok:scheme:/foo.mok!/"),
          ("mok:mok:scheme:/foo.mok!/bar.mok!/", "../..", "scheme:/"),
          ("mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "", "mok:mok:scheme:/foo.mok!/x/bar.mok!/y"),
          ("mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "..", "mok:mok:scheme:/foo.mok!/x/bar.mok!/"),
          ("mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../..", "mok:scheme:/foo.mok!/x"),
          ("mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../..", "mok:scheme:/foo.mok!/"),
          ("mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../..", "scheme:/"),

          ("file:/foo", "mok:/bar", "mok:/bar"),

          ("file:/foo", "/bar", "file:/bar"),
          ("file:/foo", "/bar/", "file:/bar"),
          ("file:/foo", "/bar.mok", "mok:file:/bar.mok!/"),
          ("file:/foo", "/bar.mok/", "mok:file:/bar.mok!/"),
          ("file:/foo", "bar", "file:/foo/bar"),
          ("file:/foo", "bar/", "file:/foo/bar"),
          ("file:/foo", "bar.mok", "mok:file:/foo/bar.mok!/"),
          ("file:/foo", "bar.mok/", "mok:file:/foo/bar.mok!/"),
          ("file:/foo", "../bar", "file:/bar"),
          ("file:/foo", "../bar/", "file:/bar"),
          ("file:/foo", "../bar.mok", "mok:file:/bar.mok!/"),
          ("file:/foo", "../bar.mok/", "mok:file:/bar.mok!/"),

          ("file:/foo/", "/bar", "file:/bar"),
          ("file:/foo/", "/bar/", "file:/bar"),
          ("file:/foo/", "/bar.mok", "mok:file:/bar.mok!/"),
          ("file:/foo/", "/bar.mok/", "mok:file:/bar.mok!/"),
          ("file:/foo/", "bar", "file:/foo/bar"),
          ("file:/foo/", "bar/", "file:/foo/bar"),
          ("file:/foo/", "bar.mok", "mok:file:/foo/bar.mok!/"),
          ("file:/foo/", "bar.mok/", "mok:file:/foo/bar.mok!/"),
          ("file:/foo/", "../bar", "file:/bar"),
          ("file:/foo/", "../bar/", "file:/bar"),
          ("file:/foo/", "../bar.mok", "mok:file:/bar.mok!/"),
          ("file:/foo/", "../bar.mok/", "mok:file:/bar.mok!/"),

          ("mok:file:/foo.mok!/", "/bar", "file:/bar"),
          ("mok:file:/foo.mok!/", "/bar/", "file:/bar"),
          ("mok:file:/foo.mok!/", "/bar.mok", "mok:file:/bar.mok!/"),
          ("mok:file:/foo.mok!/", "/bar.mok/", "mok:file:/bar.mok!/"),
          ("mok:file:/foo.mok!/", "bar", "mok:file:/foo.mok!/bar"),
          ("mok:file:/foo.mok!/", "bar/", "mok:file:/foo.mok!/bar"),
          ("mok:file:/foo.mok!/", "bar.mok", "mok:mok:file:/foo.mok!/bar.mok!/"),
          ("mok:file:/foo.mok!/", "bar.mok/", "mok:mok:file:/foo.mok!/bar.mok!/"),
          ("mok:file:/foo.mok!/", "../bar", "file:/bar"),
          ("mok:file:/foo.mok!/", "../bar/", "file:/bar"),
          ("mok:file:/foo.mok!/", "../bar.mok", "mok:file:/bar.mok!/"),
          ("mok:file:/foo.mok!/", "../bar.mok/", "mok:file:/bar.mok!/")
        ))

      def test(table: TableFor3[String, String, String]) {
        apply { resolver =>
          forAll(table) { (_base, _uri, _expected) =>
            val base = new FsNodePath(new URI(_base))
            val uri = new URI(_uri)
            val expected = new FsNodePath(new URI(_expected))
            resolver resolve (base, uri) should equal (expected)
          }
        }
      }
    }
  }

  private def apply[V](fun: TUriResolver => V) = {
    fun(
      new TUriResolver(
        new TArchiveDetector(
          TArchiveDetector.NULL,
          Array(
            Array("file", mock[FsDriver]),
            Array("mok", mock[FsArchiveDriver[_]])
          )
        )
      )
    )
  }
}
