/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.kernel.FsManager;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.util.Date;
import javax.management.*;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by a single file
 * system manager.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class JmxIOStatisticsView
extends StandardMBean
implements JmxIOStatisticsMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final JmxIOStatistics model;
    private final String type;

    static synchronized JmxIOStatisticsMXBean register(final JmxIOStatistics model, final String type) {
        final JmxIOStatisticsView view = new JmxIOStatisticsView(model, type);
        final ObjectName name = getObjectName(model, type);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxIOStatisticsMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final JmxIOStatistics model, final String type) {
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

    private static synchronized ObjectName getObjectName(final JmxIOStatistics model, final String type) {
        if (null == type)
            throw new NullPointerException();
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

    private JmxIOStatisticsView(final JmxIOStatistics model, final String name) {
        super(JmxIOStatisticsMXBean.class, true);
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