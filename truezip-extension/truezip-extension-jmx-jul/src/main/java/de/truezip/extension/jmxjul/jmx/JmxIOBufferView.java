/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import static de.truezip.kernel.cio.Entry.Access.*;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.Size.STORAGE;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.IOBuffer;
import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.management.*;

/**
 * The MXBean implementation for an {@link IOBuffer I/O pool entry}.
 *
 * @author  Christian Schlichtherle
 */
final class JmxIOBufferView
extends StandardMBean
implements JmxIOBufferViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final IOBuffer<?> model;

    static JmxIOBufferViewMXBean register(final IOBuffer<?> model) {
        final ObjectName name = getObjectName(model);
        final JmxIOBufferViewMXBean view = new JmxIOBufferView(model);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxIOBufferViewMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final IOBuffer<?> model) {
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

    private static ObjectName getObjectName(final IOBuffer<?> model) {
        final String path = model.getName();
        try {
            return new ObjectName(  IOBuffer.class.getName(),
                                    "name",
                                    ObjectName.quote(path));
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxIOBufferView(IOBuffer<?> model) {
        super(JmxIOBufferViewMXBean.class, true);
        this.model = model;
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
        if (info.getName().equals("Name")) {
            description = "The name of this I/O pool entry.";
        } else if (info.getName().equals("SizeOfData")) {
            description = "The data size of this I/O pool entry.";
        } else if (info.getName().equals("SizeOfStorage")) {
            description = "The storage size of this I/O pool entry.";
        } else if (info.getName().equals("TimeWritten")) {
            description = "The last write time of this I/O pool entry.";
        } else if (info.getName().equals("TimeRead")) {
            description = "The last read or access time of this I/O pool entry.";
        } else if (info.getName().equals("TimeCreated")) {
            description = "The creation time of this I/O pool entry.";
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
        return model.getName();
    }

    @Override
    public long getSizeOfData() {
        return model.getSize(DATA);
    }

    @Override
    public long getSizeOfStorage() {
        return model.getSize(STORAGE);
    }

    @Override
    public String getTimeWritten() {
        final long time = model.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final long time = model.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final long time = model.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }
}