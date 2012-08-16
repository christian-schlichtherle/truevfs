/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.insight.stats.SyncStatistics;

/**
 * A controller for {@linkplain SyncStatistics getSyncStats statistics}.
 * 
 * @see    JmxIoStatistics
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tSyncStatistics extends I5tStatistics {

    I5tSyncStatistics(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatisticsView newView() { return new I5tSyncStatisticsView(this); }
}
