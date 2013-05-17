/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import net.java.truevfs.comp.jmx._

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
private class I5tSyncMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int) = new I5tSyncStatistics(this, offset)

  override def rotateStats(origin: JmxComponent) {
    origin match {
      case _: I5tManager =>
      case _ => super.rotateStats(origin)
    }
  }
}
