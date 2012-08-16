/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.util.Date;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truevfs.ext.insight.stats.FsStatistics;
import net.java.truevfs.ext.insight.stats.IoStatistics;
import net.java.truevfs.ext.insight.stats.SyncStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class I5tStatisticsView extends StandardMBean {

    protected I5tStatisticsView(Class<?> type, boolean isMXBean) {
        super(type, isMXBean);
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
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
    protected String getDescription(final MBeanOperationInfo info) {
        switch (info.getName()) {
        case "rotate":
            return "Rotates the underlying statistics. This operation does not affect snapshots.";
        default:
            return null;
        }
    }

    abstract FsStatistics getStats();
    
    final IoStatistics getReadStats() {
        return getStats().readStats();
    }

    final IoStatistics getWriteStats() {
        return getStats().writeStats();
    }

    final SyncStatistics getSyncStats() {
        return getStats().syncStats();
    }

    public final int getReadBytesPerOperation() {
        return getReadStats().bytesPerOperation();
    }

    public final long getReadBytesTotal() {
        return getReadStats().bytesTotal();
    }

    public final long getReadKilobytesPerSecond() {
        return getReadStats().kilobytesPerSecond();
    }

    public final long getReadNanosecondsPerOperation() {
        return getReadStats().nanosecondsPerOperation();
    }

    public final long getReadNanosecondsTotal() {
        return getReadStats().nanosecondsTotal();
    }

    public final long getReadOperations() {
        return getReadStats().sequenceNumber();
    }

    public int getReadThreadsTotal() {
        return getReadStats().threadsTotal();
    }

    public abstract String getSubject();

    public final long getSyncNanosecondsPerOperation() {
        return getSyncStats().nanosecondsPerOperation();
    }

    public final long getSyncNanosecondsTotal() {
        return getSyncStats().nanosecondsTotal();
    }

    public final long getSyncOperations() {
        return getSyncStats().sequenceNumber();
    }

    public final int getSyncThreadsTotal() {
        return getSyncStats().threadsTotal();
    }

    public final String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    public final long getTimeCreatedMillis() {
        return getStats().timeMillis();
    }

    public final String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    public final long getTimeUpdatedMillis() {
        return Math.max(
                Math.max(
                    getReadStats().timeMillis(),
                    getWriteStats().timeMillis()),
                getSyncStats().timeMillis());
    }

    public final int getWriteBytesPerOperation() {
        return getWriteStats().bytesPerOperation();
    }

    public final long getWriteBytesTotal() {
        return getWriteStats().bytesTotal();
    }

    public final long getWriteKilobytesPerSecond() {
        return getWriteStats().kilobytesPerSecond();
    }

    public final long getWriteNanosecondsPerOperation() {
        return getWriteStats().nanosecondsPerOperation();
    }

    public final long getWriteNanosecondsTotal() {
        return getWriteStats().nanosecondsTotal();
    }

    public final long getWriteOperations() {
        return getWriteStats().sequenceNumber();
    }

    public int getWriteThreadsTotal() {
        return getWriteStats().threadsTotal();
    }

    public abstract void rotate();
}
