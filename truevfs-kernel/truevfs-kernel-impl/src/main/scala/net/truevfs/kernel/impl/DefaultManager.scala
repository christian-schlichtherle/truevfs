/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import java.util.concurrent.locks._
import java.util.concurrent.locks.ReentrantReadWriteLock._
import java.util.logging._
import javax.annotation.concurrent._
import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec.util._
import net.truevfs.kernel.spec.util.Link._
import net.truevfs.kernel.spec.util.Link.Type._
import net.truevfs.kernel.spec.util.Links._
import scala.collection.mutable.WeakHashMap

/** The default implementation of a file system manager.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class DefaultManager(
  optionalScheduleType: Type,
  lock: ReentrantReadWriteLock = new ReentrantReadWriteLock
) extends FsAbstractManager {

  def this() = this(WEAK)

  import DefaultManager._

  DefaultManager // init companion object
  assert(null ne optionalScheduleType)

  /**
   * The map of all schedulers for composite file system controllers,
   * keyed by the mount point of their respective file system model.
   */
  private[this] val controllers =
    new WeakHashMap[FsMountPoint, Link[FsController[_ <: FsModel]]]

  private[this] val readLock = lock.readLock
  private[this] val writeLock = lock.writeLock

  override def controller(driver: FsCompositeDriver, mountPoint: FsMountPoint): FsController[_ <: FsModel] = {
    try {
      readLock lock ()
      try {
        return controller0(driver, mountPoint)
      } finally {
        readLock unlock ()
      }
    } catch {
      case ex: NeedsWriteLockException =>
        writeLock lock ()
        try {
          return controller0(driver, mountPoint)
        } finally {
          writeLock unlock ()
        }
    }
  }

  private def controller0(driver: FsCompositeDriver, mountPoint: FsMountPoint): FsController[_ <: FsModel] = {
    if (null eq mountPoint.getParent) {
      val m = new FsDefaultModel(mountPoint, null)
      return driver newController (this, m, null)
    }
    target(controllers get mountPoint orNull) match {
      case null =>
        if (!writeLock.isHeldByCurrentThread) throw NeedsWriteLockException()
        val p = controller0(driver, mountPoint.getParent)
        val m = new ScheduledModel(mountPoint, p.getModel)
        val c = driver newController (this, m, p)
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
  extends FsAbstractModel(mountPoint, parent) {
    private[this] var _controller: FsController[_ <: FsModel] = _
    private[this] var _touched: Boolean = _

    def controller = _controller

    def controller_=(controller: FsController[_ <: FsModel]) {
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
          SyncShutdownHook register DefaultManager.this
        writeLock lock ()
        try {
          schedule(touched)
        } finally {
          writeLock unlock ()
        }
        _touched = touched
      }
    }

    def schedule(mandatory: Boolean) {
      assert(writeLock.isHeldByCurrentThread)
      val mountPoint = getMountPoint
      val link: Link[FsController[_ <: FsModel]] =
        (if (mandatory) STRONG else optionalScheduleType) newLink _controller
      controllers put (mountPoint, link)
    }
  } // ScheduledModel

  def newController(driver: AnyArchiveDriver, model: FsModel, parent: FsController[_ <: FsModel]) = {
    assert(!model.isInstanceOf[LockModel])
    // HC SVNT DRACONES!
    // The FalsePositiveArchiveController decorates the FrontController
    // so that the decorated controller (chain) does not need to resolve
    // operations on false positive archive files.
    new FalsePositiveArchiveController(
      new FrontController(
        driver decorate 
          asFsController(
            new BackController(driver, new LockModel(model), parent), parent)))
  }

  override def size = {
    readLock lock ()
    try {
      controllers size
    } finally {
      readLock unlock ()
    }
  }

  override def iterator = {
    import collection.JavaConverters._
    sortedControllers.iterator.asJava
  }

  private def sortedControllers = {
    readLock lock ()
    try {
      controllers.values.map(target(_)).filter(null ne _).toIndexedSeq
      .sorted(ReverseControllerOrdering)
    } finally {
      readLock unlock ()
    }
  }

  override def sync(options: SyncOptions) {
    SyncShutdownHook.cancel()
    super.sync(options)
  }
}

private object DefaultManager {
  Logger  .getLogger( classOf[DefaultManager] getName,
                      classOf[DefaultManager] getName)
          .config("banner")

  private final class FrontController(c: FsController[_ <: FsModel])
  extends FsDecoratingController[FsModel, FsController[_ <: FsModel]](c)
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
  (driver: FsArchiveDriver[E], model: LockModel, parent: FsController[_ <: FsModel])
  extends TargetArchiveController(driver, model, parent)
  with ResourceController
  with CacheController
  with SyncController
  with LockController {
    val pool = driver.getIoPool
    require(null ne pool)
  }

  /**
   * Orders file system controllers so that all file systems appear before
   * any of their parent file systems.
   */
  private object ReverseControllerOrdering extends Ordering[FsController[_ <: FsModel]] {
    override def compare(a: FsController[_ <: FsModel], b: FsController[_ <: FsModel]) =
      b.getModel.getMountPoint.toHierarchicalUri compareTo
        a.getModel.getMountPoint.toHierarchicalUri
  }
}
