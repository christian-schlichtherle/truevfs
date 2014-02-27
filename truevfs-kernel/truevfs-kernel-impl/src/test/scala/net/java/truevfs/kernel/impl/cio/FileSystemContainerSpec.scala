/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry
import net.java.truecommons.cio.Entry._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class FileSystemContainerSpec
extends WordSpec with ShouldMatchers with PropertyChecks {
  import FileSystemContainerSpec._

  private def newContainer = new FileSystemContainer[DummyEntry]

  "A path map container" should {
    "correctly persist entries" in {
      val container = newContainer

      pending
    }
  }
}

private object FileSystemContainerSpec {

  final class DummyEntry extends Entry {
    override def getName = null
    override def getSize(size: Size) = UNKNOWN
    override def getTime(access: Access) = UNKNOWN
    override def isPermitted(tµpe: Access, entity: Entity) = null
  }
}
