/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.logging._
import net.java.truecommons.shed.Visitor
import net.java.truevfs.comp.jmx._
import net.java.truevfs.ext.pacemaker.PaceManager._
import net.java.truevfs.kernel.spec._

/** A pace manager.
  * This class is thread-safe.
  *
  * @author Christian Schlichtherle
  */
private class PaceManager(mediator: PaceMediator, manager: FsManager)
extends JmxManager[PaceMediator](mediator, manager) {

  private val cache = new Cache[FsMountPoint, FsController](maximumFileSystemsMountedDefaultValue)

  private val evictedMountPoints = cache.evictedKeySet

  def maximumSize = cache.maximumSize

  def maximumSize_=(maximumSize: Int) {
    require(maximumSize >= maximumFileSystemsMountedMinimumValue)
    cache.maximumSize = maximumSize
  }

  override def newView = new PaceManagerView(this)

  /**
   * Registers access to the given controller and eventually sync()s some
   * recently accessed archive files which exceed the maximum number of mounted
   * archive files unless they are the parent of some mounted archive files.
   *
   * @param controller the accessed file system controller.
   */
  def postAccess(controller: FsController) {
    val model = controller.getModel
    if (model.isMounted)
      cache put (model.getMountPoint, controller)
    unmountEvictedArchiveFileSystems()
  }

  private def unmountEvictedArchiveFileSystems() {
    val iterator = evictedMountPoints.iterator
    if (iterator.hasNext) {
      val builder = new FsSyncExceptionBuilder
      do {
        val evictedMountPoint = iterator next ()
        val evictedControllerFilter = new FsControllerFilter(evictedMountPoint)
        // Check that neither the evicted file system nor any of its child file
        // systems are currently mounted.
        if (!(cache exists evictedControllerFilter)) {
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
              if (!isMounted)
                cache remove mountPoint
            } else {
              if (isMounted) {
                cache put (mountPoint, controller)
                assert(assertion = false, "A file system controller visitor should not cause an archive file system to get mounted.")
              }
            }
          }
        }
      }
    )
  }
}

private object PaceManager {

  val logger = new LocalizedLogger(classOf[PaceManager])
}
