/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ScalableContainerSpec extends WordSpec with ShouldMatchers {
  import ScalableContainerSpec._

  private def create = new Container[DummyEntry] with ScalableContainer[DummyEntry]

  private[this] val always = afterWord("always")

  "A scalable container" when {
    "empty" should {
      val path = "foo"
      val parentPath = null:String
      val container = create
      "have appropriate properties" in {
        container should have size (0)
        container.iterator.hasNext should be (false)
        container.entry(path) should be (null)
        container.entry(parentPath) should be (null)
        container.remove(path) should be (false)
        container.remove(parentPath) should be (false)
      }
    }
    "not empty" should {
      val path = "foo/bar"
      val parentPath = "foo"
      val entry = DummyEntry(path)
      val container = create
      container.add(path, entry)
      "have appropriate properties" in {
        container should have size (1)
        val iterator = container.iterator
        iterator.hasNext should be (true)
        iterator.next should be theSameInstanceAs (entry)
        iterator.hasNext should be (false)
        container.entry(null) should be (null)
        container.entry(path) should be theSameInstanceAs (entry)
        container.remove(path) should be (true)
      }
    }
  }

  "A scalable container" should always {
    "report illegal arguments with a RuntimeException" when {
      "adding an entry" in {
        intercept[RuntimeException] {
          create.add(null, null)
        }
      }
      "removing an entry" in {
        intercept[RuntimeException] {
          create.remove(null)
        }
      }
    }
    "persist entries" in {
      pending
    }
  }
} // ScalableContainerSpec

object ScalableContainerSpec {
  private final case class DummyEntry(name: String)
  extends ByteArrayIoBuffer(name, 0)
} // ScalableContainerSpec
