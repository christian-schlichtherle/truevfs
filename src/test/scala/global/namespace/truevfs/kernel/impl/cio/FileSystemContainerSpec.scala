/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl.cio

import global.namespace.truevfs.comp.cio.Entry
import global.namespace.truevfs.comp.cio.Entry._
import global.namespace.truevfs.kernel.impl.cio.FileSystemContainerSpec._
import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class FileSystemContainerSpec extends AnyWordSpec {

  private def newContainer = new FileSystemContainer[DummyEntry]

  "A file system container" should {
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
