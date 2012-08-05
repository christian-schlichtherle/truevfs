/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.management.*;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;
import net.java.truevfs.kernel.spec.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import net.java.truevfs.kernel.spec.cio.Entry.Size;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * The MXBean implementation for a {@link FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
final class JmxModelView
extends StandardMBean
implements JmxModelViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();
    private static final FsCompositeDriver
            DRIVER = new FsSimpleCompositeDriver(FsDriverMapLocator.SINGLETON);

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
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final java.util.Hashtable<String, String>
                table = new java.util.Hashtable<>(2 * 4 / 3 + 1);
        table.put("type", FsModel.class.getSimpleName());
        table.put("path", ObjectName.quote(path));
        try {
            return new ObjectName(
                    JmxModelView.class.getPackage().getName(),
                    table);
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
        String description = null;
        switch (info.getName()) {
        case "Mounted":
            description = "Whether or not this file system needs to get sync()ed.";
            break;
        case "MountPoint":
            description = "The mount point URI of this file system.";
            break;
        case "MountPointOfParent":
            description = "The mount point URI of the parent file system.";
            break;
        case "SizeOfData":
            description = "The data size of this file system.";
            break;
        case "SizeOfStorage":
            description = "The storage size of this file system.";
            break;
        case "TimeWritten":
            description = "The last write time of this file system.";
            break;
        case "TimeRead":
            description = "The last read or access time of this file system.";
            break;
        case "TimeCreated":
            description = "The creation time of this file system.";
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
    public String getMountPointOfParent() {
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
        final FsEntry entry = stat();
        return null == entry ? UNKNOWN : entry.getSize(type);
    }

    @Override
    public String getTimeWritten() {
        final FsEntry entry = stat();
        final long time = null == entry ? UNKNOWN : entry.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final FsEntry entry = stat();
        final long time = null == entry ? UNKNOWN : entry.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final FsEntry entry = stat();
        final long time = null == entry ? UNKNOWN : entry.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    private @CheckForNull FsEntry stat() {
        final FsMountPoint mp = model.getMountPoint();
        final FsMountPoint pmp = mp.getParent();
        try {
            final FsManager m = FsManagerLocator.SINGLETON.get();
            return null == pmp
                    ? m.controller(DRIVER, mp)
                        .stat(NONE, FsEntryName.ROOT)
                    : m.controller(DRIVER, pmp)
                        .stat(NONE, mp.getPath().getEntryName());
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void sync() throws FsSyncException {
        new FsFilteringManager(
                model.getMountPoint(),
                FsManagerLocator.SINGLETON.get()).sync(FsSyncOptions.NONE);
    }
}
