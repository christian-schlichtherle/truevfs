/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.IoStatistics;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class JmxStatisticsView extends StandardMBean {

    final JmxStatistics stats;

    JmxStatisticsView(final Class<?> iface, final JmxStatistics stats) {
        super(iface, true);
        this.stats = Objects.requireNonNull(stats);
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "Subject":
            return "The subject of this log.";
        case "TimeCreatedDate":
            return "The time this log has been created.";
        case "TimeCreatedMillis":
            return "The time this log has been created in milliseconds.";
        case "TimeUpdatedDate":
            return "The last time this log has been updated.";
        case "TimeUpdatedMillis":
            return "The last time this log has been updated in milliseconds.";
        default:
            return null;
        }
    }

    @Override
    protected String getDescription(final MBeanOperationInfo info) {
        switch (info.getName()) {
        case "snapshot":
            return "Creates a snapshot of these statistics.";
        case "rotate":
            return "Rotates the underlying statistics. This operation does not affect snapshots.";
        default:
            return null;
        }
    }

    FsStatistics getStats() { return stats.getStats(); }
    
    final IoStatistics getReadStats() {
        return getStats().getReadStats();
    }

    final IoStatistics getWriteStats() {
        return getStats().getWriteStats();
    }

    final SyncStatistics getSyncStats() {
        return getStats().getSyncStats();
    }

    public final String getSubject() {
        return stats.getSubject();
    }

    public final String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    public final long getTimeCreatedMillis() {
        return getStats().getTimeCreated();
    }

    public final String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    public final long getTimeUpdatedMillis() {
        return Math.max(
                getReadStats().getTimeUpdated(),
                getWriteStats().getTimeUpdated());
    }

    public final void rotate() {
        stats.rotate();
    }
}
