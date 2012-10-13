/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import collection.JavaConverters._
import java.net._
import net.java.truecommons.shed.Filter
import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.mock.MockitoSugar.mock
import org.scalatest.prop._
import PaceManagerTest._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class PaceManagerTest
extends WordSpec
   with ShouldMatchers
   with PropertyChecks
   with OneInstancePerTest {

  "A PaceManager" should {
    val mediator = mock[PaceMediator]
    val delegate = new TestManager
    val manager = new PaceManager(mediator, delegate)

    "have two as its default value for the maximum mounted file systems" in {
      manager.max should be (2)
    }

    "accept any other value for the maximum mounted file systems" in {
      manager.max = 3
      manager.max should be (3)
    }

    "sync() the least recently accessed and mounted controller which exceeds the maximum mounted file system limit" in {
      val controllers = {
        def newMapping(uri: String) =
          uri -> newController(newModel(newMountPoint(uri)))
        collection.mutable.LinkedHashMap(
          newMapping("p:/"),
          newMapping("a:p:/1!/"),
          newMapping("a:a:p:/1!/a!/"),
          newMapping("a:a:p:/1!/b!/"),
          newMapping("a:a:p:/1!/c!/"),
          newMapping("a:p:/2!/"),
          newMapping("a:a:p:/2!/a!/"),
          newMapping("a:a:p:/2!/b!/"),
          newMapping("a:a:p:/2!/c!/"),
          newMapping("a:p:/3!/"),
          newMapping("a:a:p:/3!/a!/"),
          newMapping("a:a:p:/3!/b!/"),
          newMapping("a:a:p:/3!/c!/")
        )
      }
      delegate.controllers = controllers.values
      manager sync (FsSyncOptions.SYNC, Filter.ACCEPT_ANY)
      val actions = {
        val none = Set.empty[String]
        Table(
          ("access", "sync"),
          ("p:/", Set("a:p:/1!/", "a:a:p:/1!/a!/", "a:a:p:/1!/b!/", "a:a:p:/1!/c!/", "a:p:/2!/", "a:a:p:/2!/a!/", "a:a:p:/2!/b!/", "a:a:p:/2!/c!/", "a:a:p:/3!/a!/")),
          ("a:p:/1!/", Set("a:a:p:/3!/b!/")),
          ("a:a:p:/1!/a!/", Set("a:p:/3!/", "a:a:p:/3!/a!/", "a:a:p:/3!/b!/", "a:a:p:/3!/c!/")),
          ("a:a:p:/1!/b!/", none),
          ("a:a:p:/1!/c!/", Set("a:a:p:/1!/a!/")),
          ("a:p:/2!/", Set("a:a:p:/1!/b!/")),
          ("a:a:p:/2!/a!/", Set("a:p:/1!/", "a:a:p:/1!/a!/", "a:a:p:/1!/b!/", "a:a:p:/1!/c!/")),
          ("a:a:p:/2!/b!/", none),
          ("a:a:p:/2!/c!/", Set("a:a:p:/2!/a!/")),
          ("a:p:/3!/", Set("a:a:p:/2!/b!/")),
          ("a:a:p:/3!/a!/", Set("a:p:/2!/", "a:a:p:/2!/a!/", "a:a:p:/2!/b!/", "a:a:p:/2!/c!/")),
          ("a:a:p:/3!/b!/", none),
          ("a:a:p:/3!/c!/", Set("a:a:p:/3!/a!/")),
          
          // Test obeying to access order, not insertion-order!
          ("a:a:p:/3!/b!/", none),
          ("a:a:p:/3!/a!/", Set("a:a:p:/3!/c!/"))
        )
      }
      forAll(actions) { (access, sync) =>
        // Stub controllers.
        controllers.values foreach { controller =>
          val model = controller.getModel // backup
          reset(controller)
          when(controller.getModel) thenReturn model
        }

        // Register access to the controller as if some file system operation
        // had been successfully completed.
        manager postAccess controllers(access)

        // Verify sync()ing of managed controllers.
        forAll(Table(("mountPoint", "controller"), controllers.toSeq: _*)) {
          (mountPoint, controller) =>
          if (sync contains mountPoint)
            verify(controller, atLeastOnce()) sync FsSyncOptions.NONE
          else
            verify(controller, never()) sync any()
        }
      }
    }
  }
}

object PaceManagerTest {

  def newMountPoint(uri: String) = new FsMountPoint(new URI(uri))

  def newModel(mountPoint: FsMountPoint) = {
    val model = mock[FsModel]
    when(model.getMountPoint) thenReturn mountPoint
    when(model.isMounted) thenReturn (null != mountPoint.getParent)
    model
  }

  def newController(model: FsModel) =
    (when(mock[FsController].getModel) thenReturn model).getMock.asInstanceOf[FsController]

  private class TestManager(var controllers: Iterable[FsController] = Iterable.empty[FsController])
  extends FsAbstractManager {
    override def newController(driver: FsArchiveDriver[_ <: FsArchiveEntry], model: FsModel, parent: FsController) =
      throw new UnsupportedOperationException

    override def controller(driver: FsMetaDriver, mountPoint: FsMountPoint) =
      throw new UnsupportedOperationException

    override def controllers(filter: Filter[_ >: FsController]) = {
      var filtered = controllers filter (filter accept _)
      new FsControllerStream() {
        override def size() = filtered.size
        override def iterator = filtered.iterator.asJava
        override def close() { filtered = null }
      }
    }
  }
}
