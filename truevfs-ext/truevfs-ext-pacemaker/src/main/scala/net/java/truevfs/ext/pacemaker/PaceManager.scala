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

  private val cachedMountPoints = mediator.cachedMountPoints
  private val evictedMountPoints = mediator.evictedMountPoints

  def maximumSize = mediator.maximumSize
  def maximumSize_=(maximumSize: Int) { mediator.maximumSize = maximumSize }

  override def newView = new PaceManagerView(this)

  /**
   * Records access to a file system after the fact and tries to unmount the
   * least-recently accessed file systems which exceed the maximum number
   * of mounted file systems.
   * A file system is never unmounted if there any open streams or channels
   * associated with it or if any of its child file systems is mounted.
   *
   * @param mountPoint the mount point of the accessed file system.
   */
  def recordAccess(mountPoint: FsMountPoint) {
    cachedMountPoints recordAccess mountPoint
    unmountEvictedArchiveFileSystems()
  }

  private def unmountEvictedArchiveFileSystems() {
    val iterator = evictedMountPoints.iterator
    if (iterator.hasNext) {
      val builder = new FsSyncExceptionBuilder
      do {
        val evictedMountPoint = iterator next ()
        val evictedMountPointFilter = FsPrefixMountPointFilter forPrefix evictedMountPoint
        // Check that neither the evicted file system nor any of its child file
        // systems is actually mounted.
        if (!(cachedMountPoints exists evictedMountPointFilter.accept)) {
          try {
            new FsSync()
              .manager(manager)
              .filter(FsControllerFilter forPrefix evictedMountPoint)
              .run()
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
