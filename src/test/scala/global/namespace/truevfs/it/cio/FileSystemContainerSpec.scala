/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.cio

import global.namespace.truevfs.comp.cio.Entry
import global.namespace.truevfs.comp.cio.Entry._
import global.namespace.truevfs.it.cio.FileSystemContainerSpec.DummyEntry
import org.scalatest.wordspec.AnyWordSpec

import java.lang
import java.util.Optional

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

    override def isPermitted(tµpe: Access, entity: Entity): Optional[lang.Boolean] = Optional.empty
  }

}
