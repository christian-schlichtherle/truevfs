/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import net.truevfs.kernel.util._
import net.truevfs.kernel.util.Link._
import net.truevfs.kernel.util.Link.Type._
import net.truevfs.kernel.util.Links._
import java.util.logging._
import scala.collection.mutable.WeakHashMap

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
private final class ArchiveManager(optionalScheduleType: Type)
extends FsManager {
  import ArchiveManager._

  ArchiveManager // init companion object
  assert(null ne optionalScheduleType)

  /**
   * The map of all schedulers for composite file system controllers,
   * keyed by the mount point of their respective file system model.
   */
  private[this] val controllers =
    new WeakHashMap[FsMountPoint, Link[AnyController]]

  def this() = this(WEAK)

  override def controller(driver: FsCompositeDriver, mountPoint: FsMountPoint) =
    synchronized(controller0(driver, mountPoint))

  private def controller0(driver: FsCompositeDriver, mountPoint: FsMountPoint): AnyController = {
    if (null eq mountPoint.getParent) {
      val m = new FsModel(mountPoint, null)
      return driver.newController(this, m, null)
    }
    target(controllers.get(mountPoint).orNull) match {
      case null =>
        val p = controller0(driver, mountPoint.getParent)
        val m = new ScheduledModel(mountPoint, p.getModel)
        val c = driver.newController(this, m, p)
        m.controller = c
        c
      case c => c
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
  private final class ScheduledModel(mountPoint: FsMountPoint, parent: FsModel)
  extends FsModel(mountPoint, parent) {
    private[this] var _controller: AnyController = _
    private[this] var _touched: Boolean = _

    def controller = _controller

    def controller_=(controller: AnyController) {
      assert(null ne controller)
      assert(!_touched)
      _controller = controller
      schedule(false)
    }

    override def isTouched = _touched

    /**
     * Schedules the file system controller for synchronization according
     * to the given touch status.
     */
    override def setTouched(touched: Boolean) {
      if (_touched != touched) {
        if (touched)
          SyncShutdownHook.register(ArchiveManager.this)
        schedule(touched)
        _touched = touched
      }
    }

    def schedule(mandatory: Boolean) {
      val mountPoint = getMountPoint
      val link: Link[AnyController] =
        (if (mandatory) STRONG else optionalScheduleType).newLink(_controller)
      ArchiveManager.this.synchronized(controllers.put(mountPoint, link))
    }
  }

  def newController(driver: AnyArchiveDriver, model: FsModel, parent: AnyController) = {
    assert(!model.isInstanceOf[LockModel])
    val lmodel = new LockModel(model)
    // HC SUNT DRACONES!
    // The FalsePositiveArchiveController decorates the FrontController
    // so that the decorated controller (chain) does not need to resolve
    // operations on false positive archive files.
    new FalsePositiveArchiveController(
      new FrontController(
        driver.decorate(
          new BackController(driver, lmodel, parent))))
  }

  override def size = synchronized(controllers.size)

  override def iterator = synchronized {
    import collection.JavaConversions._
    sortedControllers.iterator
  }

  private def sortedControllers = {
    controllers.values.map(target(_)).filter(null ne _).toIndexedSeq
    .sorted(ReverseControllerOrdering)
  }

  override def sync(options: SyncOptions) {
    SyncShutdownHook.cancel()
    super.sync(options)
  }
}

private object ArchiveManager {
  Logger  .getLogger( classOf[ArchiveManager].getName,
                      classOf[ArchiveManager].getName)
          .config("banner")

  private final class FrontController(c: AnyController)
  extends FsDecoratingController[FsModel, AnyController](c)
  with FinalizeController

  // HC SUNT DRACONES!
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
  private final class BackController[E <: FsArchiveEntry](
    driver: FsArchiveDriver[E],
    model: LockModel,
    parent: AnyController)
  extends TargetArchiveController(driver, model, parent)
  with ResourceController
  with CacheController
  with SyncController
  with LockController {
    override val pool = driver.getIoPool
    require(null ne pool)
  }

  /**
   * Orders file system controllers so that all file systems appear before
   * any of their parent file systems.
   */
  private object ReverseControllerOrdering extends Ordering[AnyController] {
    override def compare(a: AnyController, b: AnyController) =
      b.getModel.getMountPoint.toHierarchicalUri.compareTo(
        a.getModel.getMountPoint.toHierarchicalUri)
  }
}
