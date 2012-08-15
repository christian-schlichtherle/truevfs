/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.jmx.model.FsStatistics;
import net.java.truevfs.ext.jmx.model.SyncStatistics;

/**
 * A view for {@linkplain SyncStatistics getSyncStats statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncStatisticsView
extends JmxStatisticsView implements JmxSyncStatisticsMXBean {

    private final JmxStatistics stats;

    JmxSyncStatisticsView(JmxStatistics stats) {
        super(JmxSyncStatisticsMXBean.class, true);
        this.stats = Objects.requireNonNull(stats);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of sync statistics.";
    }

    @Override
    FsStatistics getStats() { return stats.getStats(); }

    @Override
    public String getSubject() { return stats.getSubject(); }

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

    @Override
    public final void rotate() {
        stats.rotate();
    }
}
