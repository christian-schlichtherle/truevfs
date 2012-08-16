/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class I5tIoMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int) = new I5tIoStatistics(this, offset)
}
