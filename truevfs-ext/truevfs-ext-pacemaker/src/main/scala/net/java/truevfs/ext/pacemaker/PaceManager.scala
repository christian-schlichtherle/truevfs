/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.util.concurrent.locks._
import java.util.{concurrent => juc}
import java.{util => ju}

import net.java.truecommons.logging._
import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.ext.pacemaker.PaceManager._
import net.java.truevfs.kernel.spec._

/** The pace manager.
  * This class is thread-safe.
  *
  * @author Christian Schlichtherle
  */
private class PaceManager(mediator: PaceMediator, manager: FsManager)
extends JmxManager[PaceMediator](mediator, manager) {

  private val evicted = new juc.ConcurrentHashMap[FsMountPoint, FsController]
  private val mounted = new MountedControllerSet(evicted)

  def max = mounted.max
  def max_=(max: Int) { mounted.max = max }

  override def newView = new PaceManagerView(this)

  /**
   * Registers access to the given controller and eventually sync()s some
   * recently accessed archive files which exceed the maximum number of mounted
   * archive files unless they are the parent of some mounted archive files.
   *
   * @param controller the accessed file system controller.
   */
  def postAccess(controller: FsController) {
    if (controller.getModel.isMounted)
      mounted add controller
    unmountEvictedArchiveFileSystems()
  }

  private def unmountEvictedArchiveFileSystems() {
    val iterator = evicted.keySet.iterator
    if (iterator.hasNext) {
      val builder = new FsSyncExceptionBuilder
      do {
        val evictedMountPoint = iterator next ()
        val evictedControllerFilter = new FsControllerFilter(evictedMountPoint)
        // Check that neither the evicted file system nor any of its child file
        // systems are currently mounted.
        if (!(mounted exists evictedControllerFilter)) {
          try {
            manager sync (evictedControllerFilter, new FsControllerSyncVisitor(FsSyncOptions.NONE))
            iterator remove ()
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
                  iterator remove ()

                  // Mark the exception for subsequent rethrowing at the end of
                  // this method.
                  builder warn e
              }
          }
        }
      } while (iterator.hasNext)
      builder check ()
    }
  }

  override def sync(filter: ControllerFilter, visitor: ControllerVisitor) {
    mounted sync (manager, filter, visitor)
  }
}

private object PaceManager {

  val logger = new LocalizedLogger(classOf[PaceManager])

  final class MountedControllerMap(evicted: juc.ConcurrentMap[FsMountPoint, FsController])
    extends ju.LinkedHashMap[FsMountPoint, FsController](HashMaps initialCapacity maximumFileSystemsMountedDefaultValue, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > max) {
        evicted put (entry.getKey, entry.getValue)
        true
      } else {
        false
      }

    override def remove(key: Object) = {
      evicted remove key
      super.remove(key)
    }

    @volatile
    private var _max = maximumFileSystemsMountedDefaultValue

    def max = _max
    def max_=(max: Int) {
      require(max >= maximumFileSystemsMountedMinimumValue)
      _max = max
    }

    def exists(filter: ControllerFilter): Boolean = {
      val i = values.iterator
      while (i.hasNext)
        if (filter accept i.next)
          return true
      false
    }
  }

  final class MountedControllerSet
  (evicted: juc.ConcurrentMap[FsMountPoint, FsController])
  (implicit lock: ReadWriteLock = new ReentrantReadWriteLock) {

    private val map = new MountedControllerMap(evicted)
    private val readLock = lock.readLock
    private val writeLock = lock.writeLock

    def max = map.max
    def max_=(max: Int) { map.max = max }

    def add(controller: FsController) {
      val mountPoint = controller.getModel.getMountPoint
      writeLocked { map put (mountPoint, controller) }
    }

    def exists(filter: ControllerFilter) = readLocked { map exists filter }

    def sync(manager: FsManager,
             filter: ControllerFilter,
             visitor: ControllerVisitor) {
      manager sync (filter,
        new Visitor[FsController, FsSyncException] {
          override def visit(controller: FsController) {
            val model = controller.getModel
            val wasMounted = model.isMounted
            try {
              visitor visit controller
            } finally {
              val isMounted = model.isMounted
              def mountPoint = model.getMountPoint
              if (wasMounted) {
                if (!isMounted) {
                  writeLocked { map remove mountPoint }
                }
              } else {
                if (isMounted) {
                  writeLocked { map put (mountPoint, controller) }
                  assert(false, "A file system controller visitor should not cause an archive file system to get mounted.")
                }
              }
            }
          }
        }
      )
    }

    private def readLocked[V] = locked[V](readLock) _

    private def writeLocked[V] = locked[V](writeLock) _

    private def locked[V](lock: Lock)(block: => V) = {
      lock lock ()
      try {
        block
      } finally {
        lock unlock ()
      }
    }
  }
}
