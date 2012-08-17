/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager

import javax.annotation.concurrent.ThreadSafe
import net.java.truevfs.comp.inst.InstrumentingManager
import net.java.truevfs.comp.jmx.JmxMediator
import net.java.truevfs.kernel.spec.FsController
import net.java.truevfs.kernel.spec.FsManager

/**
  * A mediator for the instrumentation of the TrueVFS Kernel with a
  * [[net.java.truevfs.ext.pacemanager.PaceManager]].
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private class PaceMediator extends JmxMediator[PaceMediator] {

  override def instrument(obj: FsManager) = start(new PaceManager(this, obj))

  override def instrument(origin: InstrumentingManager[PaceMediator], obj: FsController) =
    new PaceController(origin.asInstanceOf[PaceManager], obj)
}

private object PaceMediator extends PaceMediator
