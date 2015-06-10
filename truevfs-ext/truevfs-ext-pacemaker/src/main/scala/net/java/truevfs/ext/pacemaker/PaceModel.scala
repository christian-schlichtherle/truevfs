/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truevfs.comp.inst.InstrumentingModel
import net.java.truevfs.kernel.spec.FsModel

/** A pace model.
  *
  * @author Christian Schlichtherle
  */
private final class PaceModel(mediator: PaceMediator, model: FsModel)
  extends InstrumentingModel(mediator, model) {

  private val cachedMountPoints = mediator.cachedMountPoints

  override def setMounted(isMounted: Boolean) {
    val wasMounted = model.isMounted
    model setMounted isMounted
    if (wasMounted) {
      if (!isMounted)
        cachedMountPoints remove getMountPoint
    } else {
      if (isMounted)
        cachedMountPoints put (getMountPoint, model)
    }
  }
}
