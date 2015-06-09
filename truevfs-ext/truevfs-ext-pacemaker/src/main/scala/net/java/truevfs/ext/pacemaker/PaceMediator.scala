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
private class PaceMediator extends JmxMediator[PaceMediator] with Immutable {

  final val cachedModels = new Cache[FsMountPoint, FsModel](maximumFileSystemsMountedDefaultValue)
  final val evictedMountPoints = cachedModels.evictedKeySet

  final def maximumSize = cachedModels.maximumSize
  final def maximumSize_=(maximumSize: Int) {
    require(maximumSize >= maximumFileSystemsMountedMinimumValue)
    cachedModels.maximumSize = maximumSize
  }

  final override def instrument(subject: FsManager) =
    activate(new PaceManager(this, subject))

  final override def instrument(context: InstrumentingManager[PaceMediator], subject: FsController) =
    new PaceController(context.asInstanceOf[PaceManager], subject)

  final override def instrument(context: InstrumentingManager[PaceMediator], subject: FsCompositeDriver) =
    new InstrumentingCompositeDriver(this, subject)

  final override def instrument(context: InstrumentingCompositeDriver[PaceMediator], subject: FsModel) =
    new PaceModel(this, subject)
}

private object PaceMediator extends PaceMediator
