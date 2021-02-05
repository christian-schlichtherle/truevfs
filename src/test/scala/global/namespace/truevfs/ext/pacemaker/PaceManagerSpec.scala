/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker

import global.namespace.truevfs.commons.shed.{Filter, _}
import global.namespace.truevfs.ext.pacemaker.PaceManagerSpec.{Expectation, _}
import global.namespace.truevfs.kernel.api._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.net._
import java.util.Optional
import scala.collection.mutable
import scala.language.implicitConversions

/** @author Christian Schlichtherle */
class PaceManagerSpec extends AnyWordSpec {

  private trait Fixture {

    val mediator = new PaceMediator
    val delegate = new TestManager
    val manager = new PaceManager(mediator, delegate)
  }

  "A PaceManager" should {
    "have a property for the maximum mounted file systems" which {
      "has two as its default value" in new Fixture {
        manager.getMaximumSize should be(2)
      }

      "ignores a lower value" in new Fixture {
        intercept[IllegalArgumentException] {
          manager.setMaximumSize(1)
        }
      }

      "accepts a higher value" in new Fixture {
        manager.setMaximumSize(3)
        manager.getMaximumSize should be(3)
      }
    }

    "unmount the least recently accessed archive file systems which exceed the maximum number of mounted archive file systems" in new Fixture {
      val controllers: mutable.Map[FsMountPoint, FsController] = {
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

      val table: TableFor2[FsMountPoint, Expectation] = Table[FsMountPoint, Expectation](
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
          doAnswer((_: InvocationOnMock) => {
            controller.getModel setMounted false
          }) when controller sync any()
        }
        controllers.values filter expectation foreach expectation.stub

        // Simulate and register access to the controller as if some file
        // system operation had been performed.
        {
          val controller = controllers(mountPoint)
          if (null != mountPoint.getParent) {
            controller.getModel setMounted true
          }
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
          if (expectation(controller)) {
            verify(controller, atLeastOnce()) sync FsSyncOptions.NONE
          } else {
            verify(controller, never()) sync any()
          }
          verify(controller, org.mockito.Mockito.atLeast(0)).getModel
          verifyNoMoreInteractions(controller)
        }
      }

      new FsSync().manager(manager).run()
    }
  }
}

private object PaceManagerSpec {

  type ArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  type ControllerFilter = Filter[_ >: FsController]
  type ControllerVisitor[X <: Exception] = Visitor[_ >: FsController, X]

  implicit def parseMountPoint(string: String): FsMountPoint = new FsMountPoint(new URI(string))

  def model(mediator: PaceMediator, mountPoint: FsMountPoint): FsModel = {
    val parent = {
      if (mountPoint.getParent.isPresent) {
        val parent = mock[FsModel]
        when(parent.getMountPoint) thenReturn mountPoint.getParent.get
        Optional.of(parent)
      } else {
        Optional.empty
      }
    }
    mediator.instrument(null, new DefaultModel(mountPoint, parent))
  }

  def mockController(model: FsModel): FsController = {
    val controller = mock[FsController]
    when(controller.getModel) thenReturn model
    controller
  }

  private final class TestManager(var controllers: Iterable[FsController] = Iterable.empty[FsController])
    extends FsAbstractManager {

    override def newModel(context: FsDriver, mountPoint: FsMountPoint, parent: Optional[_ <: FsModel]): FsModel =
      throw new UnsupportedOperationException

    override def newController(context: ArchiveDriver, model: FsModel, parent: Optional[_ <: FsController]): FsController =
      throw new UnsupportedOperationException

    override def controller(driver: FsCompositeDriver, mountPoint: FsMountPoint): FsController =
      throw new UnsupportedOperationException

    override def accept[X <: Exception, V <: Visitor[_ >: FsController, X]](filter: ControllerFilter, visitor: V): V = {
      controllers.filter(filter.accept).foreach(visitor.visit)
      visitor
    }
  }

  private sealed abstract class Expectation(mountPoints: FsMountPoint*) extends (FsController => Boolean) {

    private val set = mountPoints.toSet

    override def apply(controller: FsController): Boolean = set contains controller.getModel.getMountPoint

    def stub(controller: FsController): Unit
  }

  private final case class Synced(mountPoints: FsMountPoint*) extends Expectation(mountPoints: _*) {

    override def stub(controller: FsController): Unit = {}
  }

  private final case class Shelved(mountPoints: FsMountPoint*) extends Expectation(mountPoints: _*) {

    override def stub(controller: FsController): Unit = {
      doThrow(
        new FsSyncException(controller.getModel.getMountPoint, new FsOpenResourceException(1, 1))
      ) when controller sync any()
    }
  }

  private final case class Discarded(mountPoints: FsMountPoint*) extends Expectation(mountPoints: _*) {

    override def stub(controller: FsController): Unit = {
      doThrow(
        new FsSyncException(controller.getModel.getMountPoint, new Exception)
      ) when controller sync any()
    }
  }

  private final class DefaultModel(mountPoint: FsMountPoint, parent: Optional[_ <: FsModel])
    extends FsAbstractModel(mountPoint, parent) {

    private var mounted: Boolean = _

    override def isMounted: Boolean = mounted

    override def setMounted(mounted: Boolean): Unit = {
      this.mounted = mounted
    }
  }

}
