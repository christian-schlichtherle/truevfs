/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.net._

import net.java.truecommons.shed.{Filter, _}
import net.java.truevfs.ext.pacemaker.PaceManagerTest._
import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.prop.PropertyChecks._

import scala.language.implicitConversions

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class PaceManagerTest extends WordSpec with OneInstancePerTest {

  "A PaceManager" should {
    val mediator = new PaceMediator
    val delegate = new TestManager
    val manager = new PaceManager(mediator, delegate)

    "have a property for the maximum mounted file systems" which {
      "has two as its default value" in {
        manager.maximumSize should be (2)
      }

      "ignores a lower value" in {
        intercept[IllegalArgumentException] { manager.maximumSize = 1 }
      }

      "accepts a higher value" in {
        manager.maximumSize = 3
        manager.maximumSize should be (3)
      }
    }

    "unmount the least recently accessed archive file systems which exceed the maximum number of mounted archive file systems" in {
      val controllers = {
        implicit def mapping(string: String): (FsMountPoint, FsController) = {
          val mountPoint = parseMountPoint(string)
          (mountPoint, mockController(model(mediator, mountPoint)))
        }
        collection.mutable.LinkedHashMap[FsMountPoint, FsController](
          "p:/",
          "a:p:/1!/",
          "a:a:p:/1!/a!/",
          "a:a:p:/1!/b!/",
          "a:a:p:/1!/c!/",
          "a:p:/2!/",
          "a:a:p:/2!/a!/",
          "a:a:p:/2!/b!/",
          "a:a:p:/2!/c!/",
          "a:p:/3!/",
          "a:a:p:/3!/a!/",
          "a:a:p:/3!/b!/",
          "a:a:p:/3!/c!/"
        )
      }
      delegate.controllers = controllers.values

      val table = Table[FsMountPoint, Expectation](
        ("access", "expectation"),

        ("p:/", Synced()),
        ("a:p:/1!/", Synced()),
        ("a:a:p:/1!/a!/", Synced()),
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
      forAll(table) { (mountPoint, expectation) =>
        // Reset controllers and stub their behavior according to the expected
        // result.
        controllers.values foreach { controller =>
          val model = controller.getModel
          reset(controller)
          when(controller.getModel) thenReturn model
          doAnswer(new Answer[Unit] {
            override def answer(invocation: InvocationOnMock) {
              controller.getModel setMounted false
            }
          }) when controller sync any()
        }
        controllers.values filter expectation foreach expectation.stub

        // Simulate and register access to the controller as if some file
        // system operation had been performed.
        {
          val controller = controllers(mountPoint)
          if (null != mountPoint.getParent)
            controller.getModel setMounted true
        }
        expectation match {
          case Synced(_*) | Shelved(_*) =>
            manager recordAccess mountPoint

          case Discarded(_*) =>
            intercept[FsSyncException] {
              manager recordAccess mountPoint
            }
        }

        // Verify sync() attempts on managed controllers.
        forAll(Table("controller", controllers.values.toSeq: _*)) { controller =>
          if (expectation(controller))
            verify(controller, atLeastOnce()) sync FsSyncOptions.NONE
          else
            verify(controller, never()) sync any()
          verify(controller, org.mockito.Mockito.atLeast(0)).getModel
          verifyNoMoreInteractions(controller)
        }
      }

      new FsSync()
        .manager(manager)
        .run()
    }
  }
}

private object PaceManagerTest {

  type ArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  type ControllerFilter = Filter[_ >: FsController]
  type ControllerVisitor[X <: Exception] = Visitor[_ >: FsController, X]

  implicit def parseMountPoint(string: String): FsMountPoint =
    new FsMountPoint(new URI(string))

  def model(mediator: PaceMediator, mountPoint: FsMountPoint) = {
    val parent =
      if (null != mountPoint.getParent) {
        val parent = mock[FsModel]
        when(parent.getMountPoint) thenReturn mountPoint.getParent
        parent
      } else {
        null
      }
    mediator instrument (null, new DefaultModel(mountPoint, parent))
  }

  def mockController(model: FsModel) = {
    val controller = mock[FsController]
    when(controller.getModel) thenReturn model
    controller
  }

  private final class TestManager(var controllers: Iterable[FsController] = Iterable.empty[FsController])
  extends FsAbstractManager {
    override def newModel(context: FsDriver, mountPoint: FsMountPoint, parent: FsModel) =
      throw new UnsupportedOperationException

    override def newController(context: ArchiveDriver, model: FsModel, parent: FsController) =
      throw new UnsupportedOperationException

    override def controller(driver: FsCompositeDriver, mountPoint: FsMountPoint) =
      throw new UnsupportedOperationException

    override def accept[X <: Exception, V <: Visitor[_ >: FsController, X]](filter: ControllerFilter, visitor: V) = {
      controllers filter filter.accept foreach visitor.visit
      visitor
    }
  }

  private sealed abstract class Expectation(mountPoints: FsMountPoint*)
  extends (FsController => Boolean) {
    private val set = mountPoints.toSet
    override def apply(controller: FsController) =
      set contains controller.getModel.getMountPoint
    def stub(controller: FsController)
  }

  private final case class Synced(mountPoints: FsMountPoint*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) { }
  }

  private final case class Shelved(mountPoints: FsMountPoint*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) {
      doThrow(
        new FsSyncException(controller.getModel.getMountPoint, new FsOpenResourceException(1, 1))
      ) when controller sync any()
    }
  }

  private final case class Discarded(mountPoints: FsMountPoint*)
  extends Expectation(mountPoints: _*) {
    override def stub(controller: FsController) {
      doThrow(
        new FsSyncException(controller.getModel.getMountPoint, new Exception)
      ) when controller sync any()
    }
  }

  private final class DefaultModel(mountPoint: FsMountPoint, parent: FsModel)
    extends FsAbstractModel(mountPoint, parent) {

    private var mounted: Boolean = _

    override def isMounted = mounted
    override def setMounted(mounted: Boolean) { this.mounted = mounted }
  }
}
