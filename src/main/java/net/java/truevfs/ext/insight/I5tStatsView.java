/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.ext.insight.stats.FsStats;
import net.java.truevfs.ext.insight.stats.FsStatsView;
import net.java.truevfs.ext.insight.stats.IoStats;
import net.java.truevfs.ext.insight.stats.SyncStats;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;

/**
 * A base view for {@link IoStats} or {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
abstract class I5tStatsView extends StandardMBean implements FsStatsView  {

    private final I5tStatsController controller;

    I5tStatsView(I5tStatsController controller, Class<?> type, boolean isMXBean) {
        super(type, isMXBean);
        assert null != controller;
        this.controller = controller;
    }

    @Override
    protected final String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
            case "ReadBytesPerOperation":
                return "The average number of bytes per read operation.";
            case "ReadBytesTotal":
                return "The total number of bytes read.";
            case "ReadKilobytesPerSecond":
                return "The average throughput for read operations.";
            case "ReadNanosecondsPerOperation":
                return "The average execution time per read operation.";
            case "ReadNanosecondsTotal":
                return "The total execution time for read operations.";
            case "ReadOperations":
                return "The total number of read operations.";
            case "ReadThreadsTotal":
                return "The total number of reading threads.";
            case "Subject":
                return "The subject of this log.";
            case "SyncNanosecondsPerOperation":
                return "The average execution time per sync operation.";
            case "SyncNanosecondsTotal":
                return "The total execution time for sync operations.";
            case "SyncOperations":
                return "The total number of sync operations.";
            case "SyncThreadsTotal":
                return "The total number of syncing threads.";
            case "TimeCreatedDate":
                return "The time this log has been created.";
            case "TimeCreatedMillis":
                return "The time this log has been created in milliseconds.";
            case "TimeUpdatedDate":
                return "The last time this log has been updated.";
            case "TimeUpdatedMillis":
                return "The last time this log has been updated in milliseconds.";
            case "WriteBytesPerOperation":
                return "The average number of bytes per write operation.";
            case "WriteBytesTotal":
                return "The total number of bytes written.";
            case "WriteKilobytesPerSecond":
                return "The average throughput for write operations.";
            case "WriteNanosecondsPerOperation":
                return "The average execution time per write operation.";
            case "WriteNanosecondsTotal":
                return "The total execution time for write operations.";
            case "WriteOperations":
                return "The total number of write operations.";
            case "WriteThreadsTotal":
                return "The total number of writing threads.";
            default:
                return null;
        }
    }

    @Override
    protected final String getDescription(final MBeanOperationInfo info) {
        return "rotate".equals(info.getName())
                ? "Rotates the underlying statistics. This operation does not affect snapshots."
                : null;
    }

    @Override
    public final FsStats getStats() {
        return controller.getStats();
    }

    @Override
    public final String getSubject() {
        return controller.getSubject();
    }

    @Override
    public final void rotate() {
        controller.rotate();
    }
}
