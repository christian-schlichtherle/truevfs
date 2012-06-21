/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.inst.jmx;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import net.truevfs.kernel.spec.FsManager;

/**
 * Provides statistics for the federated file systems managed by a single file
 * system manager.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class JmxIoStatisticsView
extends StandardMBean
implements JmxIoStatisticsMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final JmxIoStatistics model;
    private final String type;

    static synchronized JmxIoStatisticsMXBean register(final JmxIoStatistics model, final String type) {
        final JmxIoStatisticsView view = new JmxIoStatisticsView(model, type);
        final ObjectName name = getObjectName(model, type);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxIoStatisticsMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final JmxIoStatistics model, final String type) {
        final ObjectName name = getObjectName(model, type);
        try {
            try {
                mbs.unregisterMBean(name);
            } catch (InstanceNotFoundException ignored) {
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static synchronized ObjectName getObjectName(final JmxIoStatistics model, final String type) {
        Objects.requireNonNull(type);
        final long time = model.getTimeCreatedMillis();
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final java.util.Hashtable<String, String>
                table = new java.util.Hashtable<String, String>(2 * 4 / 3 + 1);
        table.put("type", type);
        //table.put("id", "" + time);
        table.put("name", ObjectName.quote(format(time)));
        try {
            return new ObjectName(FsManager.class.getName(), table);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxIoStatisticsView(final JmxIoStatistics model, final String name) {
        super(JmxIoStatisticsMXBean.class, true);
        assert null != model;
        assert null != name;
        this.model = model;
        this.type = name;
    }
    
    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mbinfo = super.getMBeanInfo();
        return new MBeanInfo(mbinfo.getClassName(),
                mbinfo.getDescription(),
                mbinfo.getAttributes(),
                mbinfo.getConstructors(),
                mbinfo.getOperations(),
                getNotificationInfo());
    }
    
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[]{};
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
        if (info.getName().equals("Type")) {
            description = "The type of these I/O statistics.";
        } else if (info.getName().equals("TimeCreated")) {
            description = "The time these I/O statistics have been created.";
        } else if (info.getName().equals("Read")) {
            description = "The number of bytes read.";
        } else if (info.getName().equals("Written")) {
            description = "The number of bytes written.";
        }
        return description;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanParameterInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        return null;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanParameterInfo.getType()
     */
    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        return null;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanOperationInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        String description = null;
        if (info.getName().equals("close")) {
            description = "Closes these I/O statistics log.";
        }
        return description;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTimeCreated() {
        return format(model.getTimeCreatedMillis());
    }

    private static String format(long time) {
        return DateFormat.getDateTimeInstance().format(new Date(time));
    }

    @Override
    public long getTimeCreatedMillis() {
        return model.getTimeCreatedMillis();
    }

    @Override
    public long getBytesRead() {
        return model.getBytesRead();
    }

    @Override
    public long getBytesWritten() {
        return model.getBytesWritten();
    }

    @Override
    public void close() {
        unregister(model, type);
    }
}