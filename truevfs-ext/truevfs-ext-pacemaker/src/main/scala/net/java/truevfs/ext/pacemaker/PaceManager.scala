/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.logging._
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

  private val cachedModels = mediator.cachedModels
  private val evictedMountPoints = mediator.evictedMountPoints

  def maximumSize = mediator.maximumSize
  def maximumSize_=(maximumSize: Int) { mediator.maximumSize = maximumSize }

  override def newView = new PaceManagerView(this)

  /**
   * Registers access to the given controller and eventually sync()s some
   * recently accessed archive files which exceed the maximum number of mounted
   * archive files unless they are the parent of some mounted archive files.
   *
   * @param controller the accessed file system controller.
   */
  def postAccess(controller: FsController) {
    // Depending on a number of preconditions, the mount point of the file
    // system may have already been added to the cache by our pace model in
    // the file system model decorator chain.
    // In this case, looking up the mount point in the cache is enough to update
    // its state with the access order of mount points.
    // Otherwise, the lookup will simply return `null` and the state of the
    // cache will be unchanged.
    cachedModels get controller.getModel.getMountPoint
    unmountEvictedArchiveFileSystems()
  }

  private def unmountEvictedArchiveFileSystems() {
    val iterator = evictedMountPoints.iterator
    if (iterator.hasNext) {
      val builder = new FsSyncExceptionBuilder
      do {
        val evictedMountPoint = iterator next ()
        val evictedModelFilter = new FsModelFilter(evictedMountPoint)
        // Check that neither the evicted file system nor any of its child file
        // systems are currently mounted.
        if (!(cachedModels existsValue evictedModelFilter)) {
          try {
            manager sync(new FsControllerFilter(evictedModelFilter), new FsControllerSyncVisitor(FsSyncOptions.NONE))
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
                  // Prevent retrying this operation - it would most likely
                  // yield the same result.
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
}

private object PaceManager {

  val logger = new LocalizedLogger(classOf[PaceManager])
}
