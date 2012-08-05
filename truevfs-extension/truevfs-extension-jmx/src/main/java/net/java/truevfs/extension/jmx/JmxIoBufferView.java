/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.management.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * The MXBean implementation for an {@link IoBuffer I/O pool entry}.
 *
 * @author Christian Schlichtherle
 */
final class JmxIoBufferView
extends StandardMBean
implements JmxIoBufferViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final IoBuffer buffer;

    static JmxIoBufferViewMXBean register(final IoBuffer model) {
        final ObjectName name = getObjectName(model);
        final JmxIoBufferViewMXBean view = new JmxIoBufferView(model);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxIoBufferViewMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final IoBuffer model) {
        final ObjectName name = getObjectName(model);
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

    private static ObjectName getObjectName(final IoBuffer model) {
        final String path = model.getName();
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final java.util.Hashtable<String, String>
                table = new java.util.Hashtable<>(2 * 4 / 3 + 1);
        table.put("type", IoBuffer.class.getSimpleName());
        table.put("path", ObjectName.quote(path));
        try {
            return new ObjectName(
                    JmxIoBufferView.class.getPackage().getName(),
                    table);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxIoBufferView(IoBuffer buffer) {
        super(JmxIoBufferViewMXBean.class, true);
        this.buffer = buffer;
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

    @Override
    protected String getDescription(MBeanInfo info) {
        return "An I/O pool entry.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        switch (info.getName()) {
        case "Name":
            description = "The name of this I/O pool entry.";
            break;
        case "SizeOfData":
            description = "The data size of this I/O pool entry.";
            break;
        case "SizeOfStorage":
            description = "The storage size of this I/O pool entry.";
            break;
        case "TimeWritten":
            description = "The last write time of this I/O pool entry.";
            break;
        case "TimeRead":
            description = "The last read or access time of this I/O pool entry.";
            break;
        case "TimeCreated":
            description = "The creation time of this I/O pool entry.";
            break;
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
     * You can supply a customized description for MBeanParameterInfo.getName()
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
        return description;
    }

    @Override
    public String getName() {
        return buffer.getName();
    }

    @Override
    public long getSizeOfData() {
        return buffer.getSize(DATA);
    }

    @Override
    public long getSizeOfStorage() {
        return buffer.getSize(STORAGE);
    }

    @Override
    public String getTimeWritten() {
        final long time = buffer.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final long time = buffer.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final long time = buffer.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }
}