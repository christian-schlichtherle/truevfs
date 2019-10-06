/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.util

import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

/** A mediator for the instrumentation of the TrueVFS Kernel with a
  * [[PaceManager]] and a [[PaceController]].
  *
  * @author Christian Schlichtherle
  */
private final class PaceMediator extends JmxMediator[PaceMediator] {

  val cachedMountPoints: LruCache[FsMountPoint] = new LruCache[FsMountPoint](maximumFileSystemsMountedDefaultValue)
  val evictedMountPoints: util.Set[FsMountPoint] = cachedMountPoints.evicted

  def maximumSize: Int = cachedMountPoints.maximumSize

  def maximumSize_=(maximumSize: Int): Unit = {
    require(maximumSize >= maximumFileSystemsMountedMinimumValue)
    cachedMountPoints.maximumSize = maximumSize
  }

  override def instrument(subject: FsManager): PaceManager = activate(new PaceManager(this, subject))

  override def instrument(context: InstrumentingManager[PaceMediator], subject: FsController): FsController = {
    new PaceController(context.asInstanceOf[PaceManager], subject)
  }

  override def instrument(context: InstrumentingManager[PaceMediator], subject: FsCompositeDriver): FsCompositeDriver = {
    new InstrumentingCompositeDriver(this, subject)
  }

  override def instrument(context: InstrumentingCompositeDriver[PaceMediator], subject: FsModel): FsModel = {
    new PaceModel(this, subject)
  }
}
