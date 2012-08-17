/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager

import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

/**
  * A mediator for the instrumentation of the TrueVFS Kernel with a
  * [[net.java.truevfs.ext.pacemanager.PaceManager]].
  *
  * @author Christian Schlichtherle
  */
private class PaceMediator extends JmxMediator[PaceMediator] with Immutable {

  override def instrument(subject: FsManager) =
    start(new PaceManager(this, subject))

  override def instrument(origin: InstrumentingManager[PaceMediator], subject: FsController) =
    new PaceController(origin.asInstanceOf[PaceManager], subject)
}

private object PaceMediator extends PaceMediator
