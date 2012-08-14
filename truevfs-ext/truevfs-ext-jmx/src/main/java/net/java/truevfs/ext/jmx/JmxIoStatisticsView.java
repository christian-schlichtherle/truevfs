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
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxIoStatisticsView
extends StandardMBean implements JmxIoStatisticsMXBean {

    private final JmxStatistics stats;

    JmxIoStatisticsView(final JmxStatistics stats) {
        super(JmxIoStatisticsMXBean.class, true);
        this.stats = Objects.requireNonNull(stats);
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of I/O statistics.";
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanAttributeInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
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

    FsStatistics getStats() { return stats.getStats(); }
    
    private IoStatistics getReadStats() {
        return getStats().getReadStats();
    }

    private IoStatistics getWriteStats() {
        return getStats().getWriteStats();
    }

    @Override
    public int getReadBytesPerOperation() {
        return getReadStats().getBytesPerOperation();
    }

    @Override
    public long getReadBytesTotal() {
        return getReadStats().getBytesTotal();
    }

    @Override
    public long getReadKilobytesPerSecond() {
        return getReadStats().getKilobytesPerSecond();
    }

    @Override
    public long getReadNanosecondsPerOperation() {
        return getReadStats().getNanosecondsPerOperation();
    }

    @Override
    public long getReadNanosecondsTotal() {
        return getReadStats().getNanosecondsTotal();
    }

    @Override
    public long getReadOperations() {
        return getReadStats().getSequenceNumber();
    }

    @Override
    public String getSubject() {
        return stats.getSubject();
    }

    @Override
    public String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeCreatedMillis() {
        return getStats().getTimeCreated();
    }

    @Override
    public String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    @Override
    public long getTimeUpdatedMillis() {
        return Math.max(
                getReadStats().getTimeUpdated(),
                getWriteStats().getTimeUpdated());
    }

    @Override
    public int getWriteBytesPerOperation() {
        return getWriteStats().getBytesPerOperation();
    }

    @Override
    public long getWriteBytesTotal() {
        return getWriteStats().getBytesTotal();
    }

    @Override
    public long getWriteKilobytesPerSecond() {
        return getWriteStats().getKilobytesPerSecond();
    }

    @Override
    public long getWriteNanosecondsPerOperation() {
        return getWriteStats().getNanosecondsPerOperation();
    }

    @Override
    public long getWriteNanosecondsTotal() {
        return getWriteStats().getNanosecondsTotal();
    }

    @Override
    public long getWriteOperations() {
        return getWriteStats().getSequenceNumber();
    }

    @Override
    public void rotate() {
        stats.rotate();
    }
}
