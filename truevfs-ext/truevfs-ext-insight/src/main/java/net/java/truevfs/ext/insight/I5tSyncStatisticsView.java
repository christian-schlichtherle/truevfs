/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.insight.stats.FsStatistics;
import net.java.truevfs.ext.insight.stats.SyncStatistics;

/**
 * A view for {@linkplain SyncStatistics getSyncStats statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tSyncStatisticsView
extends I5tStatisticsView implements I5tSyncStatisticsMXBean {

    private final I5tStatistics stats;

    I5tSyncStatisticsView(I5tStatistics stats) {
        super(I5tSyncStatisticsMXBean.class, true);
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
    public final void rotate() {
        stats.rotate();
    }
}
