/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.management.*;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;
import net.java.truevfs.kernel.spec.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
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
        case "MountPoint":
            description = "The mount point URI of this file system.";
            break;
        case "Mounted":
            description = "Whether or not this file system needs to get sync()ed.";
            break;
        case "ParentMountPoint":
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
    public String getMountPoint() {
        return model.getMountPoint().toString();
    }

    @Override
    public boolean isMounted() {
        return model.isMounted();
    }

    @Override
    public String getParentMountPoint() {
        final FsModel parent = model.getParent();
        assert null != parent;
        return parent.getMountPoint().toString();
    }

    private volatile FsController parentController;

    private FsController getParentController() {
        final FsController parentController = this.parentController;
        return null != parentController
                ? parentController
                : (this.parentController = FsManagerLocator.SINGLETON.get()
                    .controller(DRIVER, model.getMountPoint())
                    .getParent());
    }

    private volatile FsEntryName parentEntryName;

    private FsEntryName getParentEntryName() {
        final FsEntryName parentEntryName = this.parentEntryName;
        return null != parentEntryName
                ? parentEntryName
                : (this.parentEntryName = model.getMountPoint().getPath().getEntryName());
    }

    @Override
    public long getSizeOfData() {
        try {
            return getParentController()
                    .stat(NONE, getParentEntryName())
                    .getSize(DATA);
        } catch (IOException ex) {
            return UNKNOWN;
        }
    }

    @Override
    public long getSizeOfStorage() {
        try {
            return getParentController()
                    .stat(NONE, getParentEntryName())
                    .getSize(STORAGE);
        } catch (IOException ex) {
            return UNKNOWN;
        }
    }

    @Override
    public String getTimeWritten() {
        final long time;
        try {
            time = getParentController()
                        .stat(NONE, getParentEntryName())
                        .getTime(WRITE);
        } catch (IOException ex) {
            return null;
        }
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final long time;
        try {
            time = getParentController()
                        .stat(NONE, getParentEntryName())
                        .getTime(READ);
        } catch (IOException ex) {
            return null;
        }
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final long time;
        try {
            time = getParentController()
                        .stat(NONE, getParentEntryName())
                        .getTime(CREATE);
        } catch (IOException ex) {
            return null;
        }
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public void sync() throws FsSyncException {
        new FsFilteringManager( FsManagerLocator.SINGLETON.get(),
                                model.getMountPoint())
                .sync(FsSyncOptions.NONE);
    }
}