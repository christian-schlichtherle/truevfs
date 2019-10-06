/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

/** A mediator for the instrumentation of the TrueVFS Kernel with a
  * [[PaceManager]] and a [[PaceController]].
  *
  * @author Christian Schlichtherle
  */
private class PaceMediator extends JmxMediator[PaceMediator] {

  final val cachedMountPoints = new LruCache[FsMountPoint](maximumFileSystemsMountedDefaultValue)
  final val evictedMountPoints = cachedMountPoints.evicted

  final def maximumSize: Int = cachedMountPoints.maximumSize
  final def maximumSize_=(maximumSize: Int): Unit = {
    require(maximumSize >= maximumFileSystemsMountedMinimumValue)
    cachedMountPoints.maximumSize = maximumSize
  }

  final override def instrument(subject: FsManager): PaceManager =
    activate(new PaceManager(this, subject))

  final override def instrument(context: InstrumentingManager[PaceMediator], subject: FsController): FsController =
    new PaceController(context.asInstanceOf[PaceManager], subject)

  final override def instrument(context: InstrumentingManager[PaceMediator], subject: FsCompositeDriver): FsCompositeDriver =
    new InstrumentingCompositeDriver(this, subject)

  final override def instrument(context: InstrumentingCompositeDriver[PaceMediator], subject: FsModel): FsModel =
    new PaceModel(this, subject)
}

private object PaceMediator extends PaceMediator
