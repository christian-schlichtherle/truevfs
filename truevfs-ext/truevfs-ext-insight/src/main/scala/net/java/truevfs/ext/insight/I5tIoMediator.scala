/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
private class I5tIoMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int) = new I5tIoStatistics(this, offset)
}
