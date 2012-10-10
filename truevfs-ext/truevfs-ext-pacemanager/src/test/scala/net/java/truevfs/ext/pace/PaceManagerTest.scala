/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import java.net._
import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.mock._
import org.scalatest.prop._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class PaceManagerTest extends WordSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest with PropertyChecks {

  "A PaceManager" should {
    val mediator = mock[PaceMediator]
    val delegate = mock[FsManager]
    val manager = spy(new PaceManager(mediator, delegate))

    "have two as its default value for the maximum mounted file systems" in {
      manager.max should be (2)
    }

    "accept any other value for the maximum mounted file systems" in {
      manager.max = 3
      manager.max should be (3)
    }

    "sync() the least recently accessed and mounted controller which exceeds the maximum mounted file system limit" in {
      def newMountPoint(uri: String) = new FsMountPoint(new URI(uri))
      def newModel(mountPoint: FsMountPoint) = {
        val model = mock[FsModel]
        when(model.getMountPoint) thenReturn mountPoint
        when(model.isMounted) thenReturn (null != mountPoint.getParent)
        model
      }
      def newController(model: FsModel) =
        (when(mock[FsController].getModel) thenReturn model).getMock.asInstanceOf[FsController]
      def newMapping(uri: String) = {
        val mp = newMountPoint(uri)
        mp -> newController(newModel(mp))
      }
      val controllers = Map(
        newMapping("platform:/"),
        newMapping("archive:platform:/one.archive!/"),
        newMapping("archive:platform:/two.archive!/"),
        newMapping("archive:platform:/three.archive!/")
      )
      controllers foreach { case (mountPoint, controller) =>
          val model = controller.getModel
          model.getMountPoint should be (mountPoint)
          model.isMounted should be (null != mountPoint.getParent)
      }
      /*val table = Table(
        ("mountPoint"),
        ("platform://"),
        ("archive:platform://one.archive!/"),
        ("archive:platform://two.archive!/"),
        ("archive:platform://three.archive!/")
      )*/
    }
  }
}
