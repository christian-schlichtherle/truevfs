/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import net.java.truevfs.comp.jmx._

/**
 * @author Christian Schlichtherle
 */
private class I5tSyncMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int) = new I5tSyncStatistics(this, offset)

  override def rotateStats(origin: JmxComponent) {
    origin match {
      case _: I5tManager =>
      case _ => super.rotateStats(origin)
    }
  }
}
