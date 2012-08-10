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
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;

/**
 * The MXBean implementation for the I/O statistics.
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
        String description = null;
        switch (info.getName()) {
        case "Subject":
            description = "The subject of this I/O statistics log.";
            break;
        case "SequenceNumber":
            description = "The sequence number of I/O statistics log.";
            break;
        case "TimeCreated":
            description = "The time this I/O statistics log has been created.";
            break;
        case "TimeCreatedMillis":
            description = "The time this I/O statistics log has been created in milliseconds.";
            break;
        case "ReadBytesPerOperation":
            description = "The average number of bytes per read operation.";
            break;
        case "ReadBytesTotal":
            description = "The total number of bytes read.";
            break;
        case "ReadKilobytesPerSecond":
            description = "The average throughput for read operations.";
            break;
        case "ReadNanosecondsTotal":
            description = "The total execution time for read operations.";
            break;
        case "ReadOperationsTotal":
            description = "The total number of read operations.";
            break;
        case "WriteBytesPerOperation":
            description = "The average number of bytes per write operation.";
            break;
        case "WriteBytesTotal":
            description = "The total number of bytes written.";
            break;
        case "WriteKilobytesPerSecond":
            description = "The average throughput for write operations.";
            break;
        case "WriteNanosecondsTotal":
            description = "The total execution time for write operations.";
            break;
        case "WriteOperationsTotal":
            description = "The total number of write operations.";
            break;
        }
        return description;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanOperationInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        String description = null;
        if (info.getName().equals("close"))
            description = "Closes this I/O statistics log.";
        return description;
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
        return new Date(stats.getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeCreatedMillis() {
        return stats.getTimeCreatedMillis();
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

    @Override
    public void close() {
        stats.close();
    }
}
