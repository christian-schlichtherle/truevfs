/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import net.java.truecommons.shed.Link._
import net.java.truecommons.shed.Link.Type._
import net.java.truecommons.shed.Links._
import java.util.concurrent.locks._
import java.util.concurrent.locks.ReentrantReadWriteLock._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import scala.collection.mutable.WeakHashMap

/** The default implementation of a file system manager.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class DefaultManager private (
  optionalScheduleType: Type,
  override val lock: ReentrantReadWriteLock
) extends FsAbstractManager with ReentrantReadWriteLockAspect {

  private[impl] def this(optionalScheduleType: Type)
  = this(optionalScheduleType, new ReentrantReadWriteLock)

  def this() = this(WEAK)

  import DefaultManager._

  assert(null ne optionalScheduleType)

  /**
   * The map of all schedulers for composite file system controllers,
   * keyed by the mount point of their respective file system model.
   */
  private[this] val controllers =
    new WeakHashMap[FsMountPoint, Link[FsController]]

  override def newController
  (driver: AnyArchiveDriver, model: FsModel, parent: FsController): FsController = {
    assert(!model.isInstanceOf[LockModel])
    // HC SVNT DRACONES!
    // The FalsePositiveArchiveController decorates the FrontController
    // so that the decorated controller (chain) does not need to resolve
    // operations on false positive archive files.
    new FalsePositiveArchiveController(
      new FrontController(
        driver decorate 
          new ControllerAdapter(
            new BackController(driver, new LockModel(model), parent),
            parent)))
  }

  override def controller(d: FsMetaDriver, mp: FsMountPoint): FsController = {
    try {
      readLocked(controller0(d, mp))
    } catch {
      case ex: NeedsWriteLockException =>
        if (readLockedByCurrentThread) throw ex;
        writeLocked(controller0(d, mp))
    }
  }

  private def controller0(d: FsMetaDriver, mp: FsMountPoint): FsController = {
    controllers.get(mp).flatMap(l => Option[FsController](l.get)) match {
      case Some(c) => c
      case None =>
        checkWriteLockedByCurrentThread
        val p = Option(mp.getParent) map (controller0(d, _))
        val m = new ManagedModel(mp, p map (_.getModel) orNull)
        val c = d newController (this, m, p orNull)
        m init c
        c
    }
  }

  /**
   * A model which schedules its controller for
   * {@linkplain #sync(BitField) synchronization} by &quot;observing&quot; its
   * {@code touched} property.
   * <p>
   * Extending the super-class to register for updates to the {@code touched}
   * property is simpler, faster and requires a smaller memory footprint than
   * the alternative observer pattern.
   */
  private final class ManagedModel(mountPoint: FsMountPoint, parent: FsModel)
  extends FsAbstractModel(mountPoint, parent) {
    private[this] var _controller: FsController = _
    @volatile private[this] var _mounted: Boolean = _

    def init(controller: FsController) {
      assert(null ne controller)
      assert(!_mounted)
      _controller = controller
      schedule(false)
    }

    override def isMounted = _mounted

    /**
     * Schedules the file system controller for synchronization according
     * to the given mount status.
     */
    override def setMounted(mounted: Boolean) {
      writeLocked {
        if (_mounted != mounted) {
          if (mounted) SyncShutdownHook register DefaultManager.this
          schedule(mounted)
          _mounted = mounted
        }
      }
    }

    def schedule(mandatory: Boolean) {
      assert(writeLock.isHeldByCurrentThread)
      controllers += getMountPoint ->
        ((if (mandatory) STRONG else optionalScheduleType) newLink _controller)
    }
  } // ManagedModel

  override def size = readLocked(controllers.size)

  override def iterator = {
    import collection.JavaConverters._
    sortedControllers.iterator.asJava
  }

  private def sortedControllers = {
    readLocked (
      controllers
      .values
      .flatMap(l => Option(l.get))
      .toIndexedSeq
      .sorted(ReverseControllerOrdering)
    )
  }

  override def sync(options: SyncOptions) {
    SyncShutdownHook cancel ()
    super.sync(options)
  }
}

private object DefaultManager {
  private final class FrontController(c: FsController)
  extends FsDecoratingController(c)
  with FinalizeController

  // HC SVNT DRACONES!
  // The LockController extends the SyncController so that
  // the extended controller (chain) doesn't need to be thread safe.
  // The SyncController extends the CacheController because the
  // selective entry cache needs to get flushed on a NeedsSyncException.
  // The CacheController extends the ResourceController because the
  // cache entries terminate streams and channels and shall not stop the
  // extended controller (chain) from getting synced.
  // The ResourceController extends the TargetArchiveController so that
  // trying to sync the file system while any stream or channel to the
  // latter is open gets detected and properly dealt with.
  private final class BackController[E <: FsArchiveEntry]
  (driver: FsArchiveDriver[E], model: LockModel, parent: FsController)
  extends TargetArchiveController(driver, model, parent)
  with ResourceController
  with CacheController
  with SyncController
  with LockController {
    val pool = driver.getPool
    require(null ne pool)
  }

  /**
   * Orders file system controllers so that all file systems appear before
   * any of their parent file systems.
   */
  private object ReverseControllerOrdering extends Ordering[FsController] {
    override def compare(a: FsController, b: FsController) =
      b.getModel.getMountPoint.toHierarchicalUri compareTo
        a.getModel.getMountPoint.toHierarchicalUri
  }
}
