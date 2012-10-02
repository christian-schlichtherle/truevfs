/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import java.{util => ju}
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.java.truecommons.logging._
import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.sl._
import scala.math._
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
  private[this] val mounted = new MountedControllerSet(evicted)

  def max = mounted.max
  def max_=(max: Int) { mounted.max = max }

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
    val i = evicted.iterator
    if (!i.hasNext) return
    //val manager = FsManagerLocator.SINGLETON.get
    val mp = controller.getModel.getMountPoint
    while (i.hasNext) {
      val ec = i.next
      val emp = ec.getModel.getMountPoint
      val fm = new FsFilteringManager(emp, manager)
      def sync(): Boolean = {
        import collection.JavaConversions._
        for (fc <- fm) {
          val fmp = fc.getModel.getMountPoint
          if (mp == fmp || (mounted contains fmp)) {
            if (emp == fmp || (mounted contains emp)) i remove ()
            return false
          }
        }
        true
      }
      if (sync()) {
        try {
          fm sync FsSyncOptions.SYNC
          i remove ()
        } catch {
          case ex: FsSyncException =>
            ex.getCause match {
              case ex2: FsOpenIoResourceException =>
                assert(ex2.getLocal == ex2.getTotal)
                logger debug ("ignoring", ex)
              case ex2 =>
                i remove ()
                throw ex;
            }
        }
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
    if (controller.getModel.isMounted) mounted add controller
  }

  override def sync(options: BitField[FsSyncOption]) {
    evicted clear ()
    try {
      manager sync options
    } finally {
      mounted mount manager
    }
  }
}

private object PaceManager {

  val logger = new LocalizedLogger(classOf[PaceManager])

  /**
    * The key string for the system property which defines the value of the
    * constant `MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE`,
    * which is equivalent to the expression
    * `PaceManager.class.getPackage().getName() + ".maximumFileSystemsMounted"`.
    */
  val maximumFileSystemsMountedPropertyKey =
    classOf[PaceManager].getPackage.getName + ".maximumFileSystemsMounted"

  /**
    * The minimum value for the maximum number of mounted file systems,
    * which is {@value}.
    */
  val maximumFileSystemsMountedMinimumValue = 2

  /**
    * The default value for the maximum number of mounted file systems.
    * The value of this constant will be set to
    * `MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE` unless a system
    * property with the key string
    * `MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_KEY`
    * is set to a value which is greater than
    * `MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE`.
    *
    * Mind you that this constant is initialized when this interface is loaded
    * and cannot accurately reflect the value in a remote JVM instance.
    */
  val maximumFileSystemsMountedDefaultValue =
    max(maximumFileSystemsMountedMinimumValue,
        Integer getInteger (maximumFileSystemsMountedPropertyKey,
                            maximumFileSystemsMountedMinimumValue))

  private val initialCapacity =
    HashMaps initialCapacity (maximumFileSystemsMountedDefaultValue + 1)

  private[this] final class MountedControllerMap(evicted: ju.Collection[FsController])
  extends ju.LinkedHashMap[FsMountPoint, FsController](initialCapacity, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > max) evicted.add(entry.getValue) else false

    @volatile
    private var _max = maximumFileSystemsMountedDefaultValue

    def max = _max
    def max_=(max: Int) {
      if (max < maximumFileSystemsMountedMinimumValue)
        throw new IllegalArgumentException
      _max = max
    }
  } // MountedControllerMap

  private final class MountedControllerSet
  (evicted: ju.Collection[FsController])
  (implicit lock: ReentrantReadWriteLock = new ReentrantReadWriteLock) {

    assert (null ne map)

    private[this] val map = new MountedControllerMap(evicted)
    private[this] val readLock = lock.readLock
    private[this] val writeLock = lock.writeLock

    def max = map.max
    def max_=(max: Int) { map.max = max }

    private def locked[V](lock: Lock)(operation: => V) = {
      lock lock ()
      try { operation }
      finally { lock unlock () }
    }

    def contains(key: FsMountPoint) = locked(readLock)(map containsKey key)

    def add(controller: FsController) {
      val mp = controller.getModel.getMountPoint
      locked(writeLock)(map put (mp, controller))
    }

    def mount(manager: FsManager) = {
      locked(writeLock) {
        map clear ()
        import scala.collection.JavaConversions._
        for (controller <- manager; model = controller.getModel)
          if (model.isMounted) map put (model.getMountPoint, controller)
        map.size
      }
    }
  } // MountedControllerSet
}
