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

  private def create() = new TestMap

  "A path map" when {
    "empty" should {
      "have appropriate properties" in {
        val map = create()
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
        val map = create()
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

  "A path map" should {
    "throw a runtime exception" when {
      "adding a null path" in {
        evaluating {
          create() += (null: String) -> Value("")
        } should produce [RuntimeException]
      }

      "removing a null path" in {
        evaluating {
          create() -= null
        } should produce [RuntimeException]
      }
    }

    "accept null values" in {
      forAll { path: String =>
        whenever (isPath(path)) {
          val map = create()
          map += path -> null
          map(path) should be (null)
          map.valuesIterator.next should be (null)
          map -= path
          map.get(path) should be (None)
        }
      }
    }

    "correctly persist values" in {
      sealed case class Action
      final case class Add(path: String) extends Action
      final case class Remove(path: String) extends Action
      val actions = Table(
        ("action", "result"),
        (Action(), IndexedSeq()),
        (Add(""), IndexedSeq("")),
        (Add("foo"), IndexedSeq("", "foo")),
        (Add("bar"), IndexedSeq("", "bar", "foo")),
        (Remove("bar"), IndexedSeq("", "foo")),
        (Add("foo/bar"), IndexedSeq("", "foo", "foo/bar")),
        (Add("bar/foo"), IndexedSeq("", "bar/foo", "foo", "foo/bar")),
        (Remove("foo/bar"), IndexedSeq("", "bar/foo", "foo")),
        (Remove("bar/foo"), IndexedSeq("", "foo")),
        (Remove("foo"), IndexedSeq("")),
        (Remove(""), IndexedSeq())
      )
      val map = create()
      forAll(actions) { (action, result) =>
        action match {
          case Add(path)    => map += path -> Value(path)
          case Remove(path) => map -= path
          case _            =>
        }
        map should have size (result size)
        for (path <- result)
          map(path).path should be (path)
        map.valuesIterator.toSeq map (_ path) should equal (result)
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

  private type TestMap = PathMap[Value]
} // PathMapSpec
