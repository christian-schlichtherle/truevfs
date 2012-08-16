/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent.ThreadSafe
import net.java.truecommons.shed.BitField
import net.java.truevfs.comp.jmx.JmxManager
import net.java.truevfs.kernel.spec.FsManager
import net.java.truevfs.kernel.spec.FsSyncOption

/**
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class I5tManager(mediator: I5tMediator, manager: FsManager)
extends JmxManager(mediator, manager) {

  override def start {
    super.start
    mediator startAllStats this
  }

  override def sync(options: BitField[FsSyncOption]) {
    val start = System.nanoTime
    super.sync(options)
    mediator logSync (System.nanoTime - start)
    mediator rotateAllStats this
  }
}
