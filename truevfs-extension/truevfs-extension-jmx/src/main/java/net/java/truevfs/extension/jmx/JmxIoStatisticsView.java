/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.util.Date;
import java.util.Hashtable;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import net.java.truecommons.shed.HashMaps;

/**
 * Provides statistics for the federated file systems managed by a single file
 * system manager.
 *
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIoStatisticsView
extends StandardMBean implements JmxIoStatisticsMXBean {
    private final JmxIoStatistics stats;
    private final String type;

    static void register(final JmxIoStatistics model, final String type) {
        final JmxIoStatisticsView mbean = new JmxIoStatisticsView(model, type);
        final ObjectName name = getObjectName(model, type);
        JmxUtils.registerMBean(mbean, name);
    }

    static void unregister(final JmxIoStatistics model, final String type) {
        final ObjectName name = getObjectName(model, type);
        JmxUtils.unregisterMBean(name);
    }

    private static ObjectName getObjectName(
            final JmxIoStatistics model,
            final String type) {
        final long time = model.getTimeCreatedMillis();
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final java.util.Hashtable<String, String>
                table = new Hashtable<>(HashMaps.initialCapacity(2));
        table.put("type", Objects.requireNonNull(type));
        table.put("time", ObjectName.quote(new Date(time).toString()));
        try {
            return new ObjectName(
                    JmxIoStatisticsView.class.getPackage().getName(),
                    table);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxIoStatisticsView(final JmxIoStatistics stats, final String type) {
        super(JmxIoStatisticsMXBean.class, true);
        assert null != stats;
        assert null != type;
        this.stats = stats;
        this.type = type;
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
        case "Type":
            description = "The type of these I/O statistics.";
            break;
        case "TimeCreated":
            description = "The time these I/O statistics have been created.";
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
            description = "Closes these I/O statistics log.";
        return description;
    }

    @Override
    public String getType() {
        return type;
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
    public long getBytesRead() {
        return stats.getBytesRead();
    }

    @Override
    public long getBytesWritten() {
        return stats.getBytesWritten();
    }

    @Override
    public void close() {
        unregister(stats, type);
    }
}