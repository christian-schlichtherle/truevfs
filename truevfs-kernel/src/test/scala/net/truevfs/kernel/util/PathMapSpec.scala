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
class PathMapSpec
extends WordSpec with ShouldMatchers with PropertyChecks {
  import PathMapSpec._

  private def newMap = PathMap[Value]('/')

  "A path map" when {
    "empty" should {
      "have appropriate properties" in {
        val map = newMap
        forAll { path: String =>
          whenever (isPath(path)) {
            map.remove(path) should be (None)
          }
        }
        map should have size (0)
        map.iterator.hasNext should be (false)
      }
    }

    "not empty" should {
      "have appropriate properties" in {
        val map = newMap
        forAll { parentPath: String =>
          whenever (isParentPath(parentPath)) {
            val parentValue = Value(parentPath)
            map += parentPath -> parentValue
            map(parentPath) should be theSameInstanceAs(parentValue)
            map.size should be >= (1)
            val iterator = map.iterator
            iterator.hasNext should be (true)
            iterator.next should be ((parentPath, parentValue))
            iterator.hasNext should be (false)
            forAll { memberName: String =>
              whenever (isMemberName(memberName)) {
                val path = parentPath + '/' + memberName
                val value = Value(path)
                map += path -> value
                map(path) should be theSameInstanceAs(value)
                map.size should be >= (2)
                val iterator = map.iterator
                iterator.hasNext should be (true)
                iterator.next should be ((parentPath, parentValue))
                iterator.hasNext should be (true)
                iterator.next should be ((path, value))
                iterator.hasNext should be (false)
                map.remove(path) should be (Some(value))
                map.get(path) should be (None)
                map.remove(path) should be (None)
              }
            }
            map.remove(parentPath) should be (Some(parentValue))
            map.get(parentPath) should be (None)
            map.remove(parentPath) should be (None)
          }
        }
      }
    }
  }

  private def check(path: String, value: Value) {
    val map = newMap
    map += path -> value
    map(path) should be theSameInstanceAs (value)
    map should have size (1)
    val iterator = map.iterator
    iterator.hasNext should be (true)
    iterator.next should be (path -> value)
    iterator.hasNext should be (false)
    map.remove(path) should be (Some(value))
    map.get(path) should be (None)
  }

  "A path map" should {
    "accept the null path" in {
      check(null, Value(null))
    }

    "accept null values" in {
      forAll { path: String =>
        whenever (isPath(path)) {
          check(path, null)
        }
      }
    }

    "correctly persist values" in {
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
      val map = newMap
      forAll(actions) { (action, expected) =>
        val result: collection.Map[String, Value] = action match {
          case List(path) =>
            val result = collection.mutable.LinkedHashMap[String, Value]()
            for ((path, value) <- map.list(path) getOrElse (Iterable()))
              result += path -> value
            result
          case Add(path)    => map += path -> Value(path); map
          case Remove(path) => map -= path; map
          case Action()     => map
        }
        result should have size (expected size)
        for (path <- expected)
          result(path).path should be (path)
        result.values map (_ path) should equal (expected)
      }
    }
  }
} // PathMapSpec

object PathMapSpec {

  private def isParentPath(parentPath: String) =
    null != parentPath && !parentPath.isEmpty && !parentPath.endsWith("/")

  private def isMemberName(memberName: String) =
    null != memberName && !memberName.isEmpty && !memberName.contains('/')

  private def isPath(path: String) = null != path

  private final case class Value(path: String)
} // PathMapSpec
