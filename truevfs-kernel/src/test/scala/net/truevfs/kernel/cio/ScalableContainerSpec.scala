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
class ScalableContainerSpec
extends WordSpec with ShouldMatchers with PropertyChecks {
  import ScalableContainerSpec._

  private def create = new DummyEntryContainer

  "A scalable container" when {
    "empty" should {
      val path = "foo"
      val parentPath: String = null
      val container = create
      "have appropriate properties" in {
        container should have size (0)
        container.iterator.hasNext should be (false)
        container(path) should be (None)
        container(parentPath) should be (None)
        container.remove(path) should be (false)
        container.remove(parentPath) should be (false)
      }
    }

    "not empty" should {
      val path = "foo/bar"
      val parentPath = "foo"
      val entry = DummyEntry(path)
      val container = create
      container(path) = Some(entry)
      "have appropriate properties" in {
        container should have size (1)
        val iterator = container.iterator
        iterator.hasNext should be (true)
        iterator.next should be theSameInstanceAs (entry)
        iterator.hasNext should be (false)
        container(path).get should be theSameInstanceAs (entry)
        container(parentPath) should be (None)
        container(null) should be (None)
        container.remove(path) should be (true)
        container.remove(parentPath) should be (false)
      }
    }
  }

  "A scalable container" should {
    "throw a runtime exception" when {
      "adding a null entry" in {
        forAll { path: String =>
          evaluating {
            create.add(path, null)
          } should produce [RuntimeException]
          (): Unit
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
      val container = create
      forAll(actions) { (action: Action, result: IndexedSeq[String]) =>
        action match {
          case Add(path) => container(path) = Some(DummyEntry(path))
          case Remove(path) => container(path) = None
          case _ =>
        }
        container should have size (result size)
        for (path <- result)
          container(path).get.getName should be (path)
        import collection.JavaConversions._
        container.iterator.toSeq map (_ getName) should equal (result)
      }
    }
  }
} // ScalableContainerSpec

object ScalableContainerSpec {
  private final case class DummyEntry(name: String)
  extends ByteArrayIoBuffer(name, 0)

  private final class DummyEntryContainer
  extends Container[DummyEntry] with ScalableContainer[DummyEntry]
} // ScalableContainerSpec
