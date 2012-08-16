/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent.ThreadSafe
import net.java.truevfs.comp.jmx.JmxColleague

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class I5tSyncMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int) = new I5tSyncStatistics(this, offset)

  override def rotateStats(origin: JmxColleague) {
    origin match {
      case _: I5tManager =>
      case _ => super.rotateStats(origin)
    }
  }
}
