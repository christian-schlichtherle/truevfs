/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import collection.mutable.WeakHashMap
import collection.JavaConverters._
import net.java.truecommons.io.Loan._
import net.java.truecommons.shed._
import net.java.truecommons.shed.Link.Type._
import net.java.truevfs.kernel.spec._
import java.io._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import DefaultManager._

@ThreadSafe
private object DefaultManager {

  type ControllerFilter = Filter[_ >: FsController]
  type ControllerVisitor[X <: IOException] = Visitor[_ >: FsController, X]

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
  private final class BackController[E <: FsArchiveEntry](
    driver: FsArchiveDriver[E],
    model: FsModel,
    parent: FsController
  ) extends TargetArchiveController[E](driver, model, parent)
  with ResourceController[E]
  with CacheController[E]
  with SyncController[E]
  with LockController[E] {
    override val pool = driver.getPool
    require(null ne pool)
  }

  private object ReverseControllerOrdering
  extends FsControllerComparator with Ordering[FsController]
}

/** The default implementation of a file system manager.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class DefaultManager
extends FsAbstractManager
with FsManagerWithControllerFactory
with ReentrantReadWriteLockAspect {

  override val lock = new ReentrantReadWriteLock

  /**
   * The map of all schedulers for composite file system controllers,
   * keyed by the mount point of their respective file system model.
   */
  private[this] val controllers =
    new WeakHashMap[FsMountPoint, Link[FsController]]

  override def newController
  (context: AnyArchiveDriver, model: FsModel, parent: FsController) = {
    assert(!model.isInstanceOf[ArchiveModel[_]])
    // HC SVNT DRACONES!
    // The FalsePositiveArchiveController decorates the FrontController
    // so that the decorated controller (chain) does not need to resolve
    // operations on false positive archive files.
    new FalsePositiveArchiveController(
      new FrontController(
        context decorate
          new ArchiveControllerAdapter(parent,
            new BackController(context, model, parent))))
  }

  override def controller(driver: FsCompositeDriver, mountPoint: FsMountPoint): FsController = {
    try {
      readLocked(controller0(driver, mountPoint))
    } catch {
      case ex: NeedsWriteLockException =>
        if (readLockedByCurrentThread) throw ex;
        writeLocked(controller0(driver, mountPoint))
    }
  }

  private def controller0(d: FsCompositeDriver, mp: FsMountPoint): FsController = {
    controllers get mp flatMap (l => Option(l.get)) match {
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
      assert(writeLockedByCurrentThread)
      controllers += getMountPoint ->
        ((if (mandatory) STRONG else WEAK) newLink _controller)
    }
  } // ManagedModel

  override def sync(
    filter: ControllerFilter,
    visitor: ControllerVisitor[FsSyncException]
  ) {
    if (filter == Filter.ACCEPT_ANY) SyncShutdownHook remove ()
    super.sync(filter, visitor)
  }

  override def visit[X <: IOException](
    filter: ControllerFilter,
    visitor: ControllerVisitor[X]
  ) {
    readLocked(controllers.values flatMap (l => Option(l.get)))
    .filter (filter accept _)
    .toIndexedSeq
    .sorted (ReverseControllerOrdering)
    .foreach (visitor visit _)
  }
}
