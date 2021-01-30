/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.util

import net.java.truevfs.kernel.impl.util.FileSystemTest._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class FileSystemTest extends WordSpec {

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
        fs should have size 0
        fs.iterator.hasNext should equal (false)
      }
    }

    "not empty" should {
      "have appropriate properties" in {
        val fs = newFileSystem
        forAll { parentPath: String =>
          whenever (isParentPath(parentPath)) {
            val parentEntry = Entry(parentPath)
            fs += parentPath -> parentEntry
            fs(parentPath) should be theSameInstanceAs parentEntry
            fs.size should be >= 1
            val iterator = fs.iterator
            iterator.hasNext shouldBe true
            iterator.next() shouldBe parentPath -> parentEntry
            iterator.hasNext shouldBe false
            forAll { memberName: String =>
              whenever (isMemberName(memberName)) {
                val path = parentPath + '/' + memberName
                val entry = Entry(path)
                fs += path -> entry
                fs(path) should be theSameInstanceAs entry
                fs.size should be >= 2
                val iterator = fs.iterator
                iterator.hasNext shouldBe true
                iterator.next() shouldBe parentPath -> parentEntry
                iterator.hasNext shouldBe true
                iterator.next() shouldBe path -> entry
                iterator.hasNext shouldBe false
                fs.remove(path) shouldBe Some(entry)
                fs.get(path) shouldBe None
                fs.remove(path) shouldBe None
              }
            }
            fs.remove(parentPath) shouldBe Some(parentEntry)
            fs.get(parentPath) shouldBe None
            fs.remove(parentPath) shouldBe None
          }
        }
      }
    }
  }

  private def check(path: String, entry: Entry): Unit = {
    val fs = newFileSystem
    fs += path -> entry
    fs(path) should be theSameInstanceAs entry
    fs should have size 1
    val iterator = fs.iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe path -> entry
    iterator.hasNext shouldBe false
    fs.remove(path) shouldBe Some(entry)
    fs.get(path) shouldBe None
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
      sealed trait Action
      final case class NoOp() extends Action
      final case class Add(path: String) extends Action
      final case class Remove(path: String) extends Action
      final case class List(path: String) extends Action
      val actions = Table(
        ("action", "expected"),
        (NoOp(), Seq()),
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
            fs.list(path) foreach (_ foreach {
                case (pathKey, entryValue) => result += pathKey -> entryValue
            })
            result
          case Add(path)    => fs += path -> Entry(path); fs
          case Remove(path) => fs -= path; fs
          case NoOp()       => fs
        }
        result should have size expected.size
        for (path <- expected)
          result(path).path should be (path)
        result.values map (_.path) should equal (expected)
      }
    }
  }
}

object FileSystemTest {

  private def isParentPath(parentPath: String) =
    null != parentPath && !parentPath.isEmpty && !parentPath.endsWith("/")

  private def isMemberName(memberName: String) =
    null != memberName && !memberName.isEmpty && !memberName.contains('/')

  private def isPath(path: String) = null != path

  private final case class Entry(path: String)
}
