/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.StandardMBean;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * A view for {@linkplain SyncStatistics getSyncStats statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncStatisticsView
extends StandardMBean implements JmxSyncStatisticsMXBean {

    private final JmxStatistics stats;

    JmxSyncStatisticsView(final JmxStatistics stats) {
        super(JmxSyncStatisticsMXBean.class, true);
        this.stats = Objects.requireNonNull(stats);
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of sync statistics.";
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanAttributeInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "Subject":
            return "The subject of this log.";
        case "SyncNanosecondsPerOperation":
            return "The average execution time per sync operation.";
        case "SyncNanosecondsTotal":
            return "The total execution time for sync operations.";
        case "SyncOperations":
            return "The total number of sync operations.";
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

    private SyncStatistics getSync() {
        return stats.getSyncStats();
    }

    @Override
    public String getSubject() {
        return stats.getSubject();
    }

    @Override
    public long getSyncNanosecondsPerOperation() {
        return getSync().getNanosecondsPerOperation();
    }

    @Override
    public long getSyncNanosecondsTotal() {
        return getSync().getNanosecondsTotal();
    }

    @Override
    public int getSyncOperations() {
        return getSync().getSequenceNumber();
    }

    @Override
    public String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeCreatedMillis() {
        return stats.getTimeCreated();
    }

    @Override
    public String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    @Override
    public long getTimeUpdatedMillis() {
        return getSync().getTimeMillis();
    }

    @Override
    public void rotate() {
        stats.rotate();
    }
}
