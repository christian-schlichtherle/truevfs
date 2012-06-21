/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl.cio

import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks

/**
  * @author Christian Schlichtherle
  */
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
