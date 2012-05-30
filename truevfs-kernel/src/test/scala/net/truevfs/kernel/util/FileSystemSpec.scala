/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks

@RunWith(classOf[JUnitRunner])
class FileSystemSpec
extends WordSpec with ShouldMatchers with PropertyChecks {
  import FileSystemSpec._

  private def newFileSystem = FileSystem[Entry]('/')

  "A file system" when {
    "empty" should {
      "have appropriate properties" in {
        val fs = newFileSystem
        forAll { path: String =>
          whenever (isPath(path)) {
            fs.remove(path) should be (None)
          }
        }
        fs should have size (0)
        fs.iterator.hasNext should be (false)
      }
    }

    "not empty" should {
      "have appropriate properties" in {
        val fs = newFileSystem
        forAll { parentPath: String =>
          whenever (isParentPath(parentPath)) {
            val parentEntry = Entry(parentPath)
            fs += parentPath -> parentEntry 
            fs(parentPath) should be theSameInstanceAs(parentEntry )
            fs.size should be >= (1)
            val iterator = fs.iterator
            iterator.hasNext should be (true)
            iterator.next should be ((parentPath, parentEntry ))
            iterator.hasNext should be (false)
            forAll { memberName: String =>
              whenever (isMemberName(memberName)) {
                val path = parentPath + '/' + memberName
                val entry = Entry(path)
                fs += path -> entry
                fs(path) should be theSameInstanceAs(entry)
                fs.size should be >= (2)
                val iterator = fs.iterator
                iterator.hasNext should be (true)
                iterator.next should be ((parentPath, parentEntry ))
                iterator.hasNext should be (true)
                iterator.next should be ((path, entry))
                iterator.hasNext should be (false)
                fs.remove(path) should be (Some(entry))
                fs.get(path) should be (None)
                fs.remove(path) should be (None)
              }
            }
            fs.remove(parentPath) should be (Some(parentEntry ))
            fs.get(parentPath) should be (None)
            fs.remove(parentPath) should be (None)
          }
        }
      }
    }
  }

  private def check(path: String, entry: Entry) {
    val fs = newFileSystem
    fs += path -> entry
    fs(path) should be theSameInstanceAs (entry)
    fs should have size (1)
    val iterator = fs.iterator
    iterator.hasNext should be (true)
    iterator.next should be (path -> entry)
    iterator.hasNext should be (false)
    fs.remove(path) should be (Some(entry))
    fs.get(path) should be (None)
  }

  "A file system" should {
    "accept the null path" in {
      check(null, Entry(null))
    }

    "accept null entries" in {
      forAll { path: String =>
        whenever (isPath(path)) {
          check(path, null)
        }
      }
    }

    "correctly persist entries" in {
      sealed case class Action
      final case class Add(path: String) extends Action
      final case class Remove(path: String) extends Action
      final case class List(path: String) extends Action
      val actions = Table(
        ("action", "expected"),
        (Action(), Seq()),
        (Add(""), Seq("")),
        (Add("foo"), Seq("", "foo")),
        (Add("bar"), Seq("", "bar", "foo")),
        (Remove("bar"), Seq("", "foo")),
        (Add("foo/bar"), Seq("", "foo", "foo/bar")),
        (Add("bar/foo"), Seq("", "bar/foo", "foo", "foo/bar")),
        (List(null), Seq("", "foo")),
        (List(""), Seq()),
        (List("foo"), Seq("foo/bar")),
        (List("bar"), Seq("bar/foo")),
        (Remove("foo/bar"), Seq("", "bar/foo", "foo")),
        (Remove("bar/foo"), Seq("", "foo")),
        (Remove("foo"), Seq("")),
        (Remove(""), Seq())
      )
      val fs = newFileSystem
      forAll(actions) { (action, expected) =>
        val result: collection.Map[String, Entry] = action match {
          case List(path) =>
            val result = collection.mutable.LinkedHashMap[String, Entry]()
            for ((path, entry) <- fs.list(path) getOrElse (Iterable()))
              result += path -> entry
            result
          case Add(path)    => fs += path -> Entry(path); fs
          case Remove(path) => fs -= path; fs
          case Action()     => fs
        }
        result should have size (expected size)
        for (path <- expected)
          result(path).path should be (path)
        result.values map (_ path) should equal (expected)
      }
    }
  }
} // FileSystemSpec

object FileSystemSpec {

  private def isParentPath(parentPath: String) =
    null != parentPath && !parentPath.isEmpty && !parentPath.endsWith("/")

  private def isMemberName(memberName: String) =
    null != memberName && !memberName.isEmpty && !memberName.contains('/')

  private def isPath(path: String) = null != path

  private final case class Entry(path: String)
} // FileSystemSpec
