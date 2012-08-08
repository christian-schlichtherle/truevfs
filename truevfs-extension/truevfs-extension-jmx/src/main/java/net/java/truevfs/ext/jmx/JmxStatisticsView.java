/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;

/**
 * Provides statistics for the federated file systems managed by a single file
 * system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatisticsView
extends StandardMBean implements JmxStatisticsMXBean {
    protected final JmxStatisticsController stats;

    public JmxStatisticsView(final JmxStatisticsController stats) {
        super(JmxStatisticsMXBean.class, true);
        this.stats = Objects.requireNonNull(stats);
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
        return stats.getKind();
    }

    @Override
    public String getTimeCreated() {
        return stats.getTimeCreated();
    }

    @Override
    public long getTimeCreatedMillis() {
        return stats.getTimeCreatedMillis();
    }

    @Override
    public long getBytesRead() {
        return stats.getBytesRead();
    }

    @Override
    public long getBytesWritten() {
        return stats.getBytesWritten();
    }

    @Override
    public void close() {
        stats.close();
    }
}
