/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

/**
 * @author Christian Schlichtherle
 */
private class I5tIoMediator(subject: String) extends I5tMediator(subject) {

  override def newStats(offset: Int): I5tStatistics = new I5tIoStatistics(this, offset)
}
