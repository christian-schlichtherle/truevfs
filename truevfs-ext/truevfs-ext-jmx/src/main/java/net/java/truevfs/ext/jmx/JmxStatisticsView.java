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
import net.java.truevfs.ext.jmx.model.IoLogger;
import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * The combined MXBean view for an {@linkplain IoLogger I/O logger}
 * and its {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatisticsView
extends StandardMBean implements JmxStatisticsMXBean {
    protected final JmxStatistics stats;

    public JmxStatisticsView(final JmxStatistics stats) {
        super(JmxStatisticsMXBean.class, true);
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
        case "Subject":
            return "The subject of this log.";
        case "SequenceNumber":
            return "The sequence number of this log.";
        case "TimeCreated":
            return "The time this log has been created.";
        case "TimeCreatedMillis":
            return "The time this log has been created in milliseconds.";
        case "TimeUpdated":
            return "The last time this log has been updated.";
        case "TimeUpdatedMillis":
            return "The last time this log has been updated in milliseconds.";
        case "ReadBytesPerOperation":
            return "The average number of bytes per read operation.";
        case "ReadBytesTotal":
            return "The total number of bytes read.";
        case "ReadKilobytesPerSecond":
            return "The average throughput for read operations.";
        case "ReadNanosecondsTotal":
            return "The total execution time for read operations.";
        case "ReadOperationsTotal":
            return "The total number of read operations.";
        case "WriteBytesPerOperation":
            return "The average number of bytes per write operation.";
        case "WriteBytesTotal":
            return "The total number of bytes written.";
        case "WriteKilobytesPerSecond":
            return "The average throughput for write operations.";
        case "WriteNanosecondsTotal":
            return "The total execution time for write operations.";
        case "WriteOperationsTotal":
            return "The total number of write operations.";
        default:
            return null;
        }
    }

    @Override
    public String getSubject() {
        return stats.getSubject();
    }

    @Override
    public int getSequenceNumber() {
        return stats.getSequenceNumber();
    }

    @Override
    public String getTimeCreated() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeCreatedMillis() {
        return stats.getTimeCreatedMillis();
    }

    @Override
    public String getTimeUpdated() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    @Override
    public long getTimeUpdatedMillis() {
        return Math.max(
                stats.getReadStats().getTimeCreatedMillis(),
                stats.getWriteStats().getTimeCreatedMillis());
    }

    @Override
    public long getReadBytesTotal() {
        return stats.getReadStats().getBytesTotal();
    }

    @Override
    public long getReadNanosecondsTotal() {
        return stats.getReadStats().getNanosecondsTotal();
    }

    @Override
    public int getReadBytesPerOperation() {
        return stats.getReadStats().getBytesPerOperation();
    }

    @Override
    public long getReadKilobytesPerSecond() {
        return stats.getReadStats().getKilobytesPerSecond();
    }

    @Override
    public int getReadOperationsTotal() {
        return stats.getReadStats().getSequenceNumber();
    }

    @Override
    public long getWriteBytesTotal() {
        return stats.getWriteStats().getBytesTotal();
    }

    @Override
    public int getWriteBytesPerOperation() {
        return stats.getWriteStats().getBytesPerOperation();
    }

    @Override
    public long getWriteKilobytesPerSecond() {
        return stats.getWriteStats().getKilobytesPerSecond();
    }

    @Override
    public long getWriteNanosecondsTotal() {
        return stats.getWriteStats().getNanosecondsTotal();
    }

    @Override
    public int getWriteOperationsTotal() {
        return stats.getWriteStats().getSequenceNumber();
    }
}
