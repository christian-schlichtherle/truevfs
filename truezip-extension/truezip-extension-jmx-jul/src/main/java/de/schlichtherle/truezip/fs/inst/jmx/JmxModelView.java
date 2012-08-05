/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import static de.schlichtherle.truezip.entry.Entry.Access.*;
import de.schlichtherle.truezip.entry.Entry.Size;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Size.STORAGE;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.management.*;

/**
 * The MXBean implementation for a {@link FsModel file system model}.
 *
 * @author  Christian Schlichtherle
 */
final class JmxModelView
extends StandardMBean
implements JmxModelViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();
    private static final FsCompositeDriver
            DRIVER = new FsSimpleCompositeDriver(FsDriverLocator.SINGLETON);

    private final FsModel model;

    static JmxModelViewMXBean register(final FsModel model) {
        final ObjectName name = getObjectName(model);
        final JmxModelViewMXBean view = new JmxModelView(model);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxModelViewMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final FsModel model) {
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

    private static ObjectName getObjectName(final FsModel model) {
        final String path = model.getMountPoint().toHierarchicalUri().toString();
        try {
            return new ObjectName(  FsModel.class.getName(),
                                    "path",
                                    ObjectName.quote(path));
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxModelView(FsModel model) {
        super(JmxModelViewMXBean.class, true);
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
        return "A file system model.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        final String name = info.getName();
        String description = null;
        if (name.equals("MountPoint")) {
            description = "The mount point URI of this file system.";
        } else if (name.equals("Mounted")) {
            description = "Whether or not this file system needs to get sync()ed.";
        } else if (name.equals("ParentMountPoint")) {
            description = "The mount point URI of the parent file system.";
        } else if (name.equals("SizeOfData")) {
            description = "The data size of this file system.";
        } else if (name.equals("SizeOfStorage")) {
            description = "The storage size of this file system.";
        } else if (name.equals("TimeWritten")) {
            description = "The last write time of this file system.";
        } else if (name.equals("TimeRead")) {
            description = "The last read or access time of this file system.";
        } else if (name.equals("TimeCreated")) {
            description = "The creation time of this file system.";
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
        if (info.getName().equals("sync")) {
            description = "Synchronizes this file system and all enclosed file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
        }
        return description;
    }

    @Override
    public boolean isMounted() {
        return model.isMounted();
    }

    @Override
    public String getMountPoint() {
        return model.getMountPoint().toString();
    }

    @Override
    public String getParentMountPoint() {
        final FsModel parent = model.getParent();
        return null != parent ? parent.getMountPoint().toString() : null;
    }

    @Override
    public long getSizeOfData() {
        return sizeOf(DATA);
    }

    @Override
    public long getSizeOfStorage() {
        return sizeOf(STORAGE);
    }

    private long sizeOf(Size type) {
        final FsEntry entry = getEntry();
        return null == entry ? UNKNOWN : entry.getSize(type);
    }

    @Override
    public String getTimeWritten() {
        final FsEntry entry = getEntry();
        final long time = null == entry ? UNKNOWN : entry.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final FsEntry entry = getEntry();
        final long time = null == entry ? UNKNOWN : entry.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final FsEntry entry = getEntry();
        final long time = null == entry ? UNKNOWN : entry.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    private @CheckForNull FsEntry getEntry() {
        final FsMountPoint mp = model.getMountPoint();
        final FsMountPoint pmp = mp.getParent();
        try {
            final FsManager m = FsManagerLocator.SINGLETON.get();
            return null == pmp
                    ? m.getController(mp, DRIVER)
                        .getEntry(FsEntryName.ROOT)
                    : m.getController(pmp, DRIVER)
                        .getEntry(mp.getPath().getEntryName());
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void sync() throws FsSyncException {
        new FsFilteringManager( FsManagerLocator.SINGLETON.get(),
                                model.getMountPoint())
                .sync(FsSyncOptions.NONE);
    }
}
