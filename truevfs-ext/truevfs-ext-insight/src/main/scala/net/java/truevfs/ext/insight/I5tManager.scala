/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

/** @author Christian Schlichtherle */
@ThreadSafe
private final class I5tManager(mediator: I5tMediator, manager: FsManager)
extends JmxManager(mediator, manager) {

  override def activate() {
    super.activate()
    mediator activateAllStats this
  }

  override def sync(visitor: FsSyncControllerVisitor) {
    val start = System.nanoTime
    super.sync(visitor)
    mediator logSync (System.nanoTime - start)
    mediator rotateAllStats this
  }
}
