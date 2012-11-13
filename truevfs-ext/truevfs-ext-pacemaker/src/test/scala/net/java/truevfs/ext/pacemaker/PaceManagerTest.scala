/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import collection.JavaConverters._
import java.net._
import net.java.truecommons.shed._
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
      manager sync (Filter.ACCEPT_ANY,
                    new FsControllerSyncVisitor(FsSyncOptions.SYNC))

      val actions = {
        Table[String, Expectation](
          ("access", "expectation"),
          ("p:/", Synced("a:p:/1!/", "a:a:p:/1!/a!/", "a:a:p:/1!/b!/", "a:a:p:/1!/c!/", "a:p:/2!/", "a:a:p:/2!/a!/", "a:a:p:/2!/b!/", "a:a:p:/2!/c!/", "a:a:p:/3!/a!/")),
          ("a:p:/1!/", Synced("a:a:p:/3!/b!/")),
          ("a:a:p:/1!/a!/", Synced("a:p:/3!/", "a:a:p:/3!/a!/", "a:a:p:/3!/b!/", "a:a:p:/3!/c!/")),
          ("a:a:p:/1!/b!/", Synced()),
          ("a:a:p:/1!/c!/", Synced("a:a:p:/1!/a!/")),
          ("a:p:/2!/", Synced("a:a:p:/1!/b!/")),
          ("a:a:p:/2!/a!/", Synced("a:p:/1!/", "a:a:p:/1!/a!/", "a:a:p:/1!/b!/", "a:a:p:/1!/c!/")),
          ("a:a:p:/2!/b!/", Synced()),
          ("a:a:p:/2!/c!/", Synced("a:a:p:/2!/a!/")),
          ("a:p:/3!/", Synced("a:a:p:/2!/b!/")),
          ("a:a:p:/3!/a!/", Synced("a:p:/2!/", "a:a:p:/2!/a!/", "a:a:p:/2!/b!/", "a:a:p:/2!/c!/")),
          ("a:a:p:/3!/b!/", Synced()),
          ("a:a:p:/3!/c!/", Synced("a:a:p:/3!/a!/")),

          // Test obeying to access order, not insertion-order!
          ("a:a:p:/3!/b!/", Synced()),
          ("a:a:p:/3!/a!/", Shelved("a:a:p:/3!/c!/")),
          ("a:a:p:/3!/a!/", Discarded("a:a:p:/3!/c!/")),
          ("a:a:p:/3!/a!/", Synced())
        )
      }
      forAll(actions) { (access, expectation) =>
        // Reset controllers and stub their behavior according to the expected
        // result.
        controllers.values foreach
        { controller =>
          val model = controller.getModel // backup
          reset(controller)
          when(controller.getModel) thenReturn model
        }
        controllers.values filter expectation foreach (expectation stub _)

        // Register access to the controller as if some file system operation
        // had been successfully completed.
        expectation match {
          case Synced(_*) | Shelved(_*) =>
            manager postAccess controllers(access)

          case Discarded(_*) =>
            intercept[FsSyncException] {
              manager postAccess controllers(access)
            }
        }

        // Verify sync() attempts on managed controllers.
        forAll(Table(("mountPoint", "controller"), controllers.toSeq: _*))
        { (mountPoint, controller) =>
          if (expectation contains mountPoint)
            verify(controller, atLeastOnce()) sync FsSyncOptions.NONE
          else
            verify(controller, never()) sync any()
          verify(controller, atLeast(0)).getModel
          verifyNoMoreInteractions(controller)
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

    override def stream(filter: Filter[_ >: FsController]) = {
      var filtered = controllers filter (filter accept _)
      new FsControllerStream() {
        override def size() = filtered.size
        override def iterator = filtered.iterator.asJava
        override def close() { filtered = null }
      }
    }
  }

  private sealed abstract class Expectation(mountPoints: String*)
  extends (FsController => Boolean) {
    private[this] val set = mountPoints.toSet
    def contains(mountPoint: String) = set contains mountPoint
    override def apply(controller: FsController) =
      contains(controller.getModel.getMountPoint.toString)
    def stub(controller: FsController)
  }

  private case class Synced(mountPoints: String*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) { }
  }

  private case class Shelved(mountPoints: String*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) {
      doThrow(new FsSyncException(controller.getModel.getMountPoint, new FsOpenResourceException(1, 1)))
      .when(controller) sync any()
    }
  }

  private case class Discarded(mountPoints: String*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) {
      doThrow(new FsSyncException(controller.getModel.getMountPoint, new Exception))
      .when(controller) sync any()
    }
  }
}
