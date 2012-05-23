/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks

@RunWith(classOf[JUnitRunner])
class SplitterSpec extends WordSpec with ShouldMatchers with PropertyChecks {

  def splitter = new Splitter('/')

  "A splitter" should {
    "throw a runtime exception" when {
      "splitting a null path" in {
        evaluating {
          splitter(null)
        } should produce [RuntimeException]
      }
    }

    "correctly split path names" in {
      val paths = Table(
        ("path", "parentPath", "memberName"),
        ("", "", ""),
        ("foo", "", "foo"),
        ("foo/bar", "foo", "bar"),
        ("foo/bar/baz", "foo/bar", "baz")
      )
      forAll(paths) { (path, parentPath, memberName) =>
        val (pp, mn) = splitter(path)
        pp should be (parentPath)
        mn should be (memberName)
      }
    }
  }
}
