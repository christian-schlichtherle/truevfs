/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * A view for {@linkplain SyncStatistics getSyncStats statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncStatisticsView
extends JmxStatisticsView implements JmxSyncStatisticsMXBean {

    JmxSyncStatisticsView(JmxStatistics stats) {
        super(JmxSyncStatisticsMXBean.class, stats);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of sync statistics.";
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "SyncNanosecondsPerOperation":
            return "The average execution time per sync operation.";
        case "SyncNanosecondsTotal":
            return "The total execution time for sync operations.";
        case "SyncOperations":
            return "The total number of sync operations.";
        default:
            return super.getDescription(info);
        }
    }

    @Override
    public long getSyncNanosecondsPerOperation() {
        return getSyncStats().getNanosecondsPerOperation();
    }

    @Override
    public long getSyncNanosecondsTotal() {
        return getSyncStats().getNanosecondsTotal();
    }

    @Override
    public long getSyncOperations() {
        return getSyncStats().getSequenceNumber();
    }

    //@Override
    public JmxSyncStatisticsMXBean snapshot() {
        return this;
    }
}
