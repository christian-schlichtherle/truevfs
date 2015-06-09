package net.java.truevfs.ext.pacemaker

import java.{util => ju}
import java.util.{concurrent => juc}
import java.util.concurrent.locks.{Lock, ReentrantReadWriteLock, ReadWriteLock}

import net.java.truecommons.shed.{HashMaps, Visitor}
import net.java.truevfs.kernel.spec.{FsSyncException, FsManager, FsController, FsMountPoint}

/** A set of mounted file system controllers.
  *
  * @author Christian Schlichtherle
  */
private final class MountedControllerCache(initialMaximumSize: Int) {

  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val readLock = lock.readLock
  private val writeLock = lock.writeLock

  private val evicted: juc.ConcurrentMap[FsMountPoint, FsController] = new juc.ConcurrentHashMap
  private val mounted = new MountedControllerMap

  @volatile
  private var _maximumSize: Int = _

  maximumSize = initialMaximumSize

  def evictedMountPoints = evicted.keySet
  def evictedControllers = evicted.values

  def maximumSize = _maximumSize
  def maximumSize_=(maximumSize: Int) {
    require(0 <= maximumSize)
    _maximumSize = maximumSize
  }

  def add(controller: FsController) {
    val mountPoint = controller.getModel.getMountPoint
    writeLocked { mounted put (mountPoint, controller) }
  }

  def exists(filter: ControllerFilter) = readLocked { mounted exists filter }

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
                writeLocked { mounted remove mountPoint }
              }
            } else {
              if (isMounted) {
                writeLocked { mounted put (mountPoint, controller) }
                assert(assertion = false, "A file system controller visitor should not cause an archive file system to get mounted.")
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

  private final class MountedControllerMap
    extends ju.LinkedHashMap[FsMountPoint, FsController](HashMaps initialCapacity initialMaximumSize, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > maximumSize) {
        evicted put (entry.getKey, entry.getValue)
        true
      } else {
        false
      }

    override def put(key: FsMountPoint, value: FsController) = {
      evicted remove key
      super.put(key, value)
    }

    override def remove(key: Object) = {
      evicted remove key
      super.remove(key)
    }

    def exists(filter: ControllerFilter): Boolean = {
      val i = values.iterator
      while (i.hasNext)
        if (filter accept i.next)
          return true
      false
    }
  }
}
