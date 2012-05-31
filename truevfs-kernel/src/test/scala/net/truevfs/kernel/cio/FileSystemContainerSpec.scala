/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.truevfs.kernel.cio

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks
import Entry._

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
