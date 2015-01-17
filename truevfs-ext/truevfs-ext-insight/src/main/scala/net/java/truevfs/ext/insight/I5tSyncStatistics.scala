/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent.ThreadSafe

/**
 * A controller for [[net.java.truevfs.ext.insight.stats.SyncStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class I5tSyncStatistics(mediator: I5tMediator, offset: Int)
extends I5tStatistics(mediator, offset) {

  def newView = new I5tSyncStatisticsView(this)
}
