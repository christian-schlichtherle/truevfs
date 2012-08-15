/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truevfs.ext.jmx.model.FsStatistics;
import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.ext.jmx.model.SyncStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class JmxStatisticsView extends StandardMBean {

    protected JmxStatisticsView(Class<?> type, boolean isMXBean) {
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
        return getStats().getReadStats();
    }

    final IoStatistics getWriteStats() {
        return getStats().getWriteStats();
    }

    final SyncStatistics getSyncStats() {
        return getStats().getSyncStats();
    }

    public final int getReadBytesPerOperation() {
        return getReadStats().getBytesPerOperation();
    }

    public final long getReadBytesTotal() {
        return getReadStats().getBytesTotal();
    }

    public final long getReadKilobytesPerSecond() {
        return getReadStats().getKilobytesPerSecond();
    }

    public final long getReadNanosecondsPerOperation() {
        return getReadStats().getNanosecondsPerOperation();
    }

    public final long getReadNanosecondsTotal() {
        return getReadStats().getNanosecondsTotal();
    }

    public final long getReadOperations() {
        return getReadStats().getSequenceNumber();
    }

    public abstract String getSubject();

    public final String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    public final long getTimeCreatedMillis() {
        return getStats().getTimeMillis();
    }

    public final String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    public final long getTimeUpdatedMillis() {
        return Math.max(
                getReadStats().getTimeMillis(),
                getWriteStats().getTimeMillis());
    }

    public final int getWriteBytesPerOperation() {
        return getWriteStats().getBytesPerOperation();
    }

    public final long getWriteBytesTotal() {
        return getWriteStats().getBytesTotal();
    }

    public final long getWriteKilobytesPerSecond() {
        return getWriteStats().getKilobytesPerSecond();
    }

    public final long getWriteNanosecondsPerOperation() {
        return getWriteStats().getNanosecondsPerOperation();
    }

    public final long getWriteNanosecondsTotal() {
        return getWriteStats().getNanosecondsTotal();
    }

    public final long getWriteOperations() {
        return getWriteStats().getSequenceNumber();
    }
    
    public abstract void rotate();
}
