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

  private def create = new Container[DummyEntry] with ScalableContainer[DummyEntry]

  "A scalable container" when {
    "empty" should {
      val path = "foo"
      val parentPath: String = null
      val container = create
      "have appropriate properties" in {
        container should have size (0)
        container.iterator.hasNext should be (false)
        container(path) should be (null)
        container(parentPath) should be (null)
        container.remove(path) should be (false)
        container.remove(parentPath) should be (false)
      }
    }

    "not empty" should {
      val path = "foo/bar"
      val parentPath = "foo"
      val entry = DummyEntry(path)
      val container = create
      container(path) = entry
      "have appropriate properties" in {
        container should have size (1)
        val iterator = container.iterator
        iterator.hasNext should be (true)
        iterator.next should be theSameInstanceAs (entry)
        iterator.hasNext should be (false)
        container(path) should be theSameInstanceAs (entry)
        container(parentPath) should be (null)
        container(null) should be (null)
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

    "persist entries correctly" in {
      sealed class Action(path: String)
      final case class Add(path: String) extends Action(path)
      final case class Remove(path: String) extends Action(path)
      val actions = Table(
        ("action", "result"),
        (Add("foo"), IndexedSeq("foo")),
        (Add("bar"), IndexedSeq("bar", "foo"))
      )
      val container = create
      forAll(actions) { (action: Action, result: IndexedSeq[String]) =>
        action match {
          case Add(path) => container(path) = DummyEntry(path)
          case Remove(path) => container(path) = null
          case _ =>
        }
        container should have size (result size)
        for (path <- result)
          container(path).getName should be (path)
        import collection.JavaConversions._
        container.iterator.toSeq.map(_ getName) should equal (result)
      }
    }
  }
} // ScalableContainerSpec

object ScalableContainerSpec {
  private final case class DummyEntry(name: String)
  extends ByteArrayIoBuffer(name, 0)
} // ScalableContainerSpec
