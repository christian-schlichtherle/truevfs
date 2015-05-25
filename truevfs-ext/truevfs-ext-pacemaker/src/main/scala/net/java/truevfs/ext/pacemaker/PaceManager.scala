/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.{util => ju}
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.java.truecommons.logging._
import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._
import scala.math._
import PaceManager._

/** The pace manager.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private class PaceManager(mediator: PaceMediator, manager: FsManager)
extends JmxManager[PaceMediator](mediator, manager) {

  private[this] val evicted = new ConcurrentLinkedQueue[FsController]
  private[this] val mounted = new MountedControllerSet(evicted)

  def max = mounted.max
  def max_=(max: Int) { mounted.max = max }

  protected override def newView = new PaceManagerView(this)

  /**
   * Registers access to the given controller and eventually sync()s some
   * recently accessed archive files which exceed the maximum number of mounted
   * archive files unless they are the parent of some mounted archive files.
   *
   * @param accessedController the accessed file system controller.
   */
  private[pacemaker] def postAccess(accessedController: FsController) {
    if (accessedController.getModel.isMounted) mounted add accessedController
    val i = evicted.iterator
    if (!i.hasNext) return
    val builder = new FsSyncExceptionBuilder
    do {
      val evictedController = i.next
      val evictedFilter = new FsControllerFilter(mountPoint(evictedController))
      if (!(mounted exists evictedFilter)) {
        try {
          manager sync (evictedFilter, new FsControllerSyncVisitor(FsSyncOptions.NONE))
          i remove ()
        } catch {
          case e: FsSyncException =>
            e.getCause match {
              case _: FsOpenResourceException =>
                // Do NOT remove evicted controller - the sync shall get
                // retried at the next call to this method!
                //it remove ()

                // This is pretty much a normal situation, so just log the
                // exception at the TRACE level.
                logger trace ("ignoring", e)
              case _ =>
                // Prevent retrying this operation - it would most likely yield
                // the same result.
                i remove ()

                // Mark the exception for subsequent rethrowing at the end of
                // this method.
                builder warn e
            }
        }
      }
    } while (i.hasNext)
    builder check ()
  }

  override def sync(filter: AnyControllerFilter,
                    visitor: ControllerSyncVisitor) {
    val i = evicted.iterator
    while (i.hasNext) if (filter accept i.next) i remove ()
    mounted sync (manager, filter, visitor)
  }
}

private object PaceManager {

  private type AnyControllerFilter = Filter[_ >: FsController]
  private type ControllerSyncVisitor = Visitor[_ >: FsController, FsSyncException]

  private val logger = new LocalizedLogger(classOf[PaceManager])

  /**
   * The key string for the system property which defines the value of the
   * constant `MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE`,
   * which is equivalent to the expression
   * `PaceManager.class.getPackage().getName() + ".maximumFileSystemsMounted"`.
   */
  private val maximumFileSystemsMountedPropertyKey =
    classOf[PaceManager].getPackage.getName + ".maximumFileSystemsMounted"

  /** The minimum value for the maximum number of mounted file systems. */
  private val maximumFileSystemsMountedMinimumValue = 2

  /**
   * The default value for the maximum number of mounted file systems.
   * The value of this constant will be set to
   * `maximumFileSystemsMountedMinimumValue` unless a system
   * property with the key string
   * `maximumFileSystemsMountedPropertyKey`
   * is set to a value which is greater than
   * `maximumFileSystemsMountedMinimumValue`.
   *
   * Mind you that this constant is initialized when this interface is loaded
   * and cannot accurately reflect the value in a remote JVM instance.
   */
  private val maximumFileSystemsMountedDefaultValue =
    max(maximumFileSystemsMountedMinimumValue,
      Integer getInteger (maximumFileSystemsMountedPropertyKey,
        maximumFileSystemsMountedMinimumValue))

  private val initialCapacity =
    HashMaps initialCapacity (maximumFileSystemsMountedDefaultValue + 1)

  private def locked[V](lock: Lock)(operation: => V) = {
    lock lock ()
    try { operation }
    finally { lock unlock () }
  }

  private def mountPoint(controller: FsController) =
    controller.getModel.getMountPoint

  private final class MountedControllerMap(evicted: ju.Collection[FsController])
    extends ju.LinkedHashMap[FsMountPoint, FsController](initialCapacity, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > max) evicted.add(entry.getValue) else false

    @volatile
    private[this] var _max = maximumFileSystemsMountedDefaultValue

    def max = _max
    def max_=(max: Int) {
      require(max >= maximumFileSystemsMountedMinimumValue)
      _max = max
    }

    def exists(filter: AnyControllerFilter): Boolean = {
      val i = values.iterator
      while (i.hasNext)
        if (filter accept i.next)
          return true
      false
    }
  } // MountedControllerMap

  private final class MountedControllerSet
  (evicted: ju.Collection[FsController])
  (implicit lock: ReentrantReadWriteLock = new ReentrantReadWriteLock) {

    private[this] val map = new MountedControllerMap(evicted)
    private[this] val readLock = lock.readLock
    private[this] val writeLock = lock.writeLock

    def max = map.max
    def max_=(max: Int) { map.max = max }

    def exists(filter: AnyControllerFilter) =
      readLocked { map exists filter }

    def add(controller: FsController) {
      val mp = mountPoint(controller)
      writeLocked { map put (mp, controller) }
    }

    def sync(manager: FsManager,
             filter: AnyControllerFilter,
             visitor: ControllerSyncVisitor) {
      manager sync (
        new Filter[FsController] {
          override def accept(controller: FsController) = {
            val accepted = filter accept controller
            if (accepted) {
              val mp = mountPoint(controller)
              writeLocked { map remove mp }
            }
            accepted
          }
        },
        new Visitor[FsController, FsSyncException] {
          override def visit(controller: FsController) {
            try {
              visitor visit controller
            } finally {
              val model = controller.getModel
              if (model.isMounted) {
                val mp = model.getMountPoint
                writeLocked { map put (mp, controller) }
              }
            }
          }
        }
        )
    }

    def readLocked[V] = locked[V](readLock)_
    def writeLocked[V] = locked[V](writeLock)_
  } // MountedControllerSet
}
