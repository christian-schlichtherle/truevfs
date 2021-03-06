/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truecommons.cio.Entry
import net.java.truecommons.cio.Entry._
import net.java.truevfs.kernel.impl.cio.FileSystemContainerTest._
import org.scalatest._

/** @author Christian Schlichtherle */
class FileSystemContainerTest extends WordSpec {

  private def newContainer = new FileSystemContainer[DummyEntry]

  "A file system container" should {
    "correctly persist entries" in {
      val container = newContainer

      pending
    }
  }
}

private object FileSystemContainerTest {

  final class DummyEntry extends Entry {
    override def getName = null
    override def getSize(size: Size) = UNKNOWN
    override def getTime(access: Access) = UNKNOWN
    override def isPermitted(tµpe: Access, entity: Entity) = null
  }
}
