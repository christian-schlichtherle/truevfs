/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager

import java.{util => ju}
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.sl._
import PaceManager._

/**
 * The pace manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class PaceManager(mediator: PaceMediator, manager: FsManager)
extends JmxManager[PaceMediator](mediator, manager) {

  private[this] val evicted = new ConcurrentLinkedQueue[FsController]
  private[this] val mounted = new MountedControllerMap(evicted)
  private[this] val accessed = new AccessedControllerSet(mounted)

  def maximumFileSystemsMounted = mounted.max

  def maximumFileSystemsMounted_=(max: Int) {
    if (max < PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE)
      throw new IllegalArgumentException
    mounted.max = max
  }

  protected override def newView = new PaceManagerView(this)

  /**
   * If the number of mounted archive files exceeds `maximumFileSystemsMounted`,
   * then this method `sync`s the least recently used (LRU) archive files which
   * exceed this value.
   *
   * @param  controller the controller for the file system to retain mounted
   *                    for subsequent access.
   */
  def retain(controller: FsController) {
    val it = evicted.iterator
    if (!it.hasNext) return
    val manager = FsManagerLocator.SINGLETON.get
    val mp = controller.getModel.getMountPoint
    while (it.hasNext) {
      val ec = it.next
      val emp = ec.getModel.getMountPoint
      val fm = new FsFilteringManager(emp, manager)
      def sync(): Boolean = {
        import collection.JavaConversions._
        for (fc <- fm) {
          val fmp = fc.getModel.getMountPoint
          if (mp == fmp || (accessed contains fmp)) {
            if (emp == fmp || (accessed contains emp)) it remove ()
            return false
          }
        }
        true
      }
      if (sync()) {
        it remove () // even if subsequent sync fails!
        fm sync FsSyncOptions.SYNC
      }
    }
  }

  /**
   * Registers the archive file system of the given controller as the most
   * recently used (MRU).
   *
   * @param controller the controller for the most recently used file system.
   */
  def accessed(controller: FsController) {
    if (controller.getModel.isMounted) accessed add controller
  }

  override def sync(options: BitField[FsSyncOption]) {
    evicted clear ()
    try {
      manager sync options
    } finally {
      accessed mount manager
    }
  }
}

private object PaceManager {
  private val initialCapacity = HashMaps.initialCapacity(
    PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE + 1)

  private final class MountedControllerMap(evicted: ju.Collection[FsController])
  extends ju.LinkedHashMap[FsMountPoint, FsController](initialCapacity, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > max) evicted.add(entry.getValue) else false

    @volatile
    var max = PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE
  } // MountedControllerMap

  private final class AccessedControllerSet(
    mounted: ju.Map[FsMountPoint, FsController],
    lock: ReentrantReadWriteLock = new ReentrantReadWriteLock
  ) {

    assert (null ne mounted)

    private[this] val readLock = lock.readLock
    private[this] val writeLock = lock.writeLock

    private def locked[V](lock: Lock)(operation: => V) = {
      lock lock ()
      try {
        operation
      } finally {
        lock unlock ()
      }
    }

    def contains(key: FsMountPoint) = locked(readLock)(mounted containsKey key)

    def add(controller: FsController) {
      val mp = controller.getModel.getMountPoint
      locked(writeLock)(mounted.put(mp, controller))
    }

    def mount(manager: FsManager) = {
      locked(writeLock) {
        mounted clear ()
        import scala.collection.JavaConversions._
        for (controller <- manager; model = controller.getModel)
          if (model.isMounted) mounted.put(model.getMountPoint, controller)
        mounted.size
      }
    }
  } // AccessedControllerSet
}
