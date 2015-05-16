/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

private object PaceMediator extends PaceMediator

/** A mediator for the instrumentation of the TrueVFS Kernel with a
  * [[PaceManager]] and a [[PaceController]].
  *
  * @author Christian Schlichtherle
  */
private class PaceMediator extends JmxMediator[PaceMediator] with Immutable {

  override def instrument(subject: FsManager) =
    activate(new PaceManager(this, subject))

  override def instrument(origin: InstrumentingManager[PaceMediator], subject: FsController) =
    new PaceController(origin.asInstanceOf[PaceManager], subject)
}
