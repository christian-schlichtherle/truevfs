/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * A controller for {@linkplain SyncStatistics sync statistics}.
 * 
 * @see    JmxIoStatistics
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncStatistics extends JmxStatistics<JmxSyncStatisticsMXBean> {

    JmxSyncStatistics(JmxMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    JmxSyncStatisticsMXBean newView() { return new JmxSyncStatisticsView(this); }
}
