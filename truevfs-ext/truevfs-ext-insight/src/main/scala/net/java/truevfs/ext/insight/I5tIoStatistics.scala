/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent.ThreadSafe

/**
 * A controller for [[net.java.truevfs.ext.insight.stats.IoStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class I5tIoStatistics(mediator: I5tMediator, offset: Int)
extends I5tStatistics(mediator, offset) {

  def newView = new I5tIoStatisticsView(this)
}
