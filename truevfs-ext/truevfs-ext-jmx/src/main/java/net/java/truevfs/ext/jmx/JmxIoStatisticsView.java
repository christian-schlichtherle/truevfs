/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * A view for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxIoStatisticsView
extends JmxStatisticsView implements JmxIoStatisticsMXBean {

    JmxIoStatisticsView(JmxStatistics stats) {
        super(JmxIoStatisticsMXBean.class, stats);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of I/O statistics.";
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
            return super.getDescription(info);
        }
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

    //@Override
    public JmxIoStatisticsMXBean snapshot() {
        return this;
    }
}
