/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.insight.stats.FsStatistics;
import net.java.truevfs.ext.insight.stats.IoStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
class I5tIoStatisticsView
extends I5tStatisticsView implements I5tIoStatisticsMXBean {

    private final I5tStatistics stats;

    I5tIoStatisticsView(I5tStatistics stats) {
        super(I5tIoStatisticsMXBean.class, true);
        this.stats = Objects.requireNonNull(stats);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of I/O statistics.";
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
