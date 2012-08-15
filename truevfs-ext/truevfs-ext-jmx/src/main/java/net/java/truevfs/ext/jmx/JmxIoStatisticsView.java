/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
class JmxIoStatisticsView
extends JmxStatisticsView implements JmxIoStatisticsMXBean {

    private final JmxStatistics stats;

    JmxIoStatisticsView(JmxStatistics stats) {
        super(JmxIoStatisticsMXBean.class, true);
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
