/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import collection.JavaConverters._
import java.{util => ju}
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.java.truecommons.io.Loan._
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
   * archive files unless they are the parent of some most recently accessed
   * archive files.
   * 
   * @param controller the file system controller to access.
   */
  def postAccess(ac: FsController) {
    if (ac.getModel.isMounted) mounted add ac
    val it = evicted.iterator
    if (!it.hasNext) return
    val builder = new FsSyncExceptionBuilder
    do {
      val ec = it.next // evicted controller
      val emp = ec.getModel.getMountPoint // evicted mount point
      val ef = new FsControllerFilter(emp) // evicted filter
      if (!(mounted exists ef)) {
        try {
          manager sync (FsSyncOptions.NONE, ef)
          it remove ()
        } catch {
          case ex: FsSyncException =>
            ex.getCause match {
              case _: FsOpenResourceException =>
                // Do NOT remove evicted controller - the sync shall get
                // retried at the call to this method!
                //it remove ()

                // This is pretty much a normal situation, so just log the
                // exception at the TRACE level.
                logger trace ("ignoring", ex)
              case _ =>
                // Prevent retrying this operation - it would most likely yield
                // the same result.
                it remove ()

                // Mark the exception for subsequent rethrowing at the end of
                // this method.
                builder warn ex
            }
        }
      }
    } while (it.hasNext)
    builder check ()
  }

  override def sync(options: BitField[FsSyncOption], filter: Filter[_ >: FsController]) {
    {
      val it = evicted.iterator
      while (it.hasNext) if (filter accept it.next) it remove ()
    }
    mounted sync (manager, options, filter)
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

  private def locked[V](lock: Lock)(operation: => V) = {
    lock lock ()
    try { operation }
    finally { lock unlock () }
  }

  implicit private def function2filter(function: FsController => Boolean) =
    new Filter[FsController] {
      def accept(controller: FsController) = function(controller)
    }

  private final class MountedControllerMap(evicted: ju.Collection[FsController])
  extends ju.LinkedHashMap[FsMountPoint, FsController](initialCapacity, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[FsMountPoint, FsController]) =
      if (size > max) evicted.add(entry.getValue) else false

    @volatile
    private[this] var _max = maximumFileSystemsMountedDefaultValue

    def max = _max
    def max_=(max: Int) {
      require (max >= maximumFileSystemsMountedMinimumValue)
      _max = max
    }

    def exists(filter: Filter[FsController]): Boolean = {
      val it = values.iterator
      while (it.hasNext) if (filter accept it.next) return true
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

    def exists(filter: Filter[FsController]) =
      locked(readLock)(map exists filter)

    def add(controller: FsController) {
      val mp = controller.getModel.getMountPoint
      locked(writeLock)(map put (mp, controller))
    }

    def sync(manager: FsManager, options: BitField[FsSyncOption], filter: Filter[_ >: FsController]) {
      locked(writeLock) {
        try {
          manager sync (options, { controller: FsController =>
              val accepted = filter accept controller
              if (accepted) map remove controller.getModel.getMountPoint
              accepted
            }
          )
        } finally {
          loan(manager controllers filter) to { stream =>
            for (controller <- stream.asScala; model = controller.getModel)
              if (model.isMounted) map put (model.getMountPoint, controller)
          }
        }
      }
    }
  } // MountedControllerSet
}
