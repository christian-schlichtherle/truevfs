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
class TreeSpec
extends WordSpec with ShouldMatchers with PropertyChecks {
  import TreeSpec._

  private def create() = new TestTree

  "A tree" when {
    "empty" should {
      val path = "foo"
      val tree = create()
      "have appropriate properties" in {
        tree should have size (0)
        tree.iterator.hasNext should be (false)
        tree(path) should be (None)
        tree.remove(path) should be (false)
      }
    }

    "not empty" should {
      val path = "foo/bar"
      val parentPath = "foo"
      val entry = TestEntry(path)
      val tree = create()
      tree(path) = Some(entry)
      "have appropriate properties" in {
        tree should have size (1)
        val iterator = tree.iterator
        iterator.hasNext should be (true)
        iterator.next should be theSameInstanceAs (entry)
        iterator.hasNext should be (false)
        tree(path).get should be theSameInstanceAs (entry)
        tree.remove(path) should be (true)
        tree(parentPath) should be (None)
        tree.remove(parentPath) should be (false)
      }
    }
  }

  "A tree" should {
    "throw a runtime exception" when {
      "adding a null path" in {
        evaluating {
          create().add(null, TestEntry(""))
        } should produce [RuntimeException]
        evaluating {
          create(null) = Some(TestEntry(""))
        } should produce [RuntimeException]
      }

      "removing a null path" in {
        evaluating {
          create().remove(null)
        } should produce [RuntimeException]
        evaluating {
          create(null) = None
        } should produce [RuntimeException]
      }

      "adding a null entry" in {
        forAll { path: String =>
          whenever (null ne path) {
            evaluating {
              create().add(path, null)
            } should produce [RuntimeException]
            (): Unit
          }
        }
      }
    }

    "correctly persist entries" in {
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
      val tree = create()
      forAll(actions) { (action, result) =>
        action match {
          case Add(path)    => tree(path) = Some(TestEntry(path))
          case Remove(path) => tree(path) = None
          case _            =>
        }
        tree should have size (result size)
        for (path <- result)
          tree(path).get.name should be (path)
        import collection.JavaConversions._
        tree.iterator.toSeq map (_ name) should equal (result)
      }
    }
  }
} // TreeSpec

object TreeSpec {
  private final case class TestEntry(name: String)
  private type TestTree = Tree[TestEntry]
} // TreeSpec
