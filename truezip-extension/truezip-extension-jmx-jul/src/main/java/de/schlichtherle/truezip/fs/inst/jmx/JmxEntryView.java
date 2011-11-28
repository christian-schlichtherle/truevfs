/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * The MXBean implementation for an {@link Entry I/O pool entry}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class JmxEntryView
extends StandardMBean
implements JmxEntryViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final Entry<?> model;

    static JmxEntryViewMXBean register(final Entry<?> model) {
        final ObjectName name = getObjectName(model);
        final JmxEntryViewMXBean view = new JmxEntryView(model);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxEntryViewMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final Entry<?> model) {
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

    private static ObjectName getObjectName(final Entry<?> model) {
        final String path = model.getName();
        try {
            return new ObjectName(  Entry.class.getName(),
                                    "name",
                                    ObjectName.quote(path));
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxEntryView(Entry<?> model) {
        super(JmxEntryViewMXBean.class, true);
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
