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
 * The MXBean view for {@linkplain SyncStatistics sync statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxSyncStatisticsView
extends StandardMBean implements JmxSyncStatisticsMXBean {

    protected final JmxSyncStatistics stats;

    public JmxSyncStatisticsView(final JmxSyncStatistics stats) {
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
        case "TimeCreatedMillis":
            return "The time this log has been created in milliseconds.";
        case "TimeCreatedString":
            return "The time this log has been created.";
        case "TimeUpdatedMillis":
            return "The last time this log has been updated in milliseconds.";
        case "TimeUpdatedString":
            return "The last time this log has been updated.";
        default:
            return null;
        }
    }

    private SyncStatistics getSyncStats() {
        return stats.getStats().getSyncStats();
    }

    @Override
    public String getSubject() {
        return stats.getSubject();
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
    public int getSyncOperations() {
        return getSyncStats().getSequenceNumber();
    }

    @Override
    public long getTimeCreatedMillis() {
        return stats.getStats().getTimeMillis();
    }

    @Override
    public String getTimeCreatedString() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeUpdatedMillis() {
        return getSyncStats().getTimeMillis();
    }

    @Override
    public String getTimeUpdatedString() {
        return new Date(getTimeUpdatedMillis()).toString();
    }
}
