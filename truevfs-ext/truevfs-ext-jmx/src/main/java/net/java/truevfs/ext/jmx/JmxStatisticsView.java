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
    protected final JmxStatistics sampler;

    public JmxStatisticsView(final JmxStatistics sampler) {
        super(JmxStatisticsMXBean.class, true);
        this.sampler = Objects.requireNonNull(sampler);
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        return "A record of I/O statistics.";
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanAttributeInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        switch (info.getName()) {
        case "Kind":
            description = "The kind of these I/O statistics.";
            break;
        case "TimeCreated":
            description = "The time these I/O statistics have been created.";
            break;
        case "TimeCreatedMillis":
            description = "The time these I/O statistics have been created in milliseconds.";
            break;
        case "BytesRead":
            description = "The number of bytes read.";
            break;
        case "BytesWritten":
            description = "The number of bytes written.";
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
            description = "Closes these I/O statistics.";
        return description;
    }

    @Override
    public String getKind() {
        return sampler.getKindString();
    }

    @Override
    public int getSequenceNumber() {
        return sampler.getSequenceNumber();
    }

    @Override
    public String getTimeCreated() {
        return new Date(sampler.getTimeCreatedMillis()).toString();
    }

    @Override
    public long getTimeCreatedMillis() {
        return sampler.getTimeCreatedMillis();
    }

    @Override
    public long getReadSumOfBytes() {
        return sampler.getReadStats().getSumOfBytes();
    }

    @Override
    public int getReadBytesPerOperation() {
        return sampler.getReadStats().getAverageBytesPerOperation();
    }

    @Override
    public long getReadKilobytesPerSecond() {
        return sampler.getReadStats().getKilobytesPerSecond();
    }

    @Override
    public int getReadNumberOfOperations() {
        return sampler.getReadStats().getSequenceNumber();
    }

    @Override
    public long getWriteSumOfBytes() {
        return sampler.getWriteStats().getSumOfBytes();
    }

    @Override
    public int getWriteBytesPerOperation() {
        return sampler.getWriteStats().getAverageBytesPerOperation();
    }

    @Override
    public long getWriteKilobytesPerSecond() {
        return sampler.getWriteStats().getKilobytesPerSecond();
    }

    @Override
    public int getWriteNumberOfOperations() {
        return sampler.getWriteStats().getSequenceNumber();
    }

    @Override
    public void close() {
        sampler.close();
    }
}
