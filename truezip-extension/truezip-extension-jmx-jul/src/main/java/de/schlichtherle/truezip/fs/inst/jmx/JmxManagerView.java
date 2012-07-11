/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOptions;
import java.lang.management.ManagementFactory;
import javax.management.*;

/**
 * The MXBean implementation for a {@link FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
final class JmxManagerView
extends StandardMBean
implements JmxManagerViewMXBean {

    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    private final FsManager model;

    static JmxManagerViewMXBean register(final FsManager model) {
        final JmxManagerViewMXBean view = new JmxManagerView(model);
        final ObjectName name = getObjectName(model);
        try {
            try {
                mbs.registerMBean(view, name);
                return view;
            } catch (InstanceAlreadyExistsException ignored) {
                return JMX.newMXBeanProxy(mbs, name, JmxManagerViewMXBean.class);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final FsManager model) {
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

    private static ObjectName getObjectName(final FsManager model) {
        final Class<?> clazz = model.getClass();
        try {
            return new ObjectName(  clazz.getPackage().getName(),
                                    "type",
                                    clazz.getSimpleName());
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxManagerView(FsManager model) {
        super(JmxManagerViewMXBean.class, true);
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
        return "The federated file system manager.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        final String name = info.getName();
        String description = null;
        if (name.equals("FileSystemsTotal")) {
            description = "The total number of file systems.";
        } else if (name.equals("FileSystemsMounted")) {
            description = "The number of file systems which have been mounted and need sync()ing.";
        } else if (name.equals("TopLevelArchiveFileSystemsTotal")) {
            description = "The total number of top level archive file systems.";
        } else if (name.equals("TopLevelArchiveFileSystemsMounted")) {
            description = "The number of top level archive file systems which have been mounted and need sync()ing.";
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
        final String name = info.getName();
        String description = null;
        if (name.equals("sync")) {
            description = "Synchronizes all managed file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
        } else if (name.equals("clearStatistics")) {
            description = "Clears all but the last I/O statistics.";
        }
        return description;
    }

    @Override
    public int getFileSystemsTotal() {
        return model.getSize();
    }

    @Override
    public int getFileSystemsMounted() {
        int mounted = 0;
        for (FsController<?> controller : model)
            if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        int total = 0;
        for (FsController<?> controller : model)
            if (isTopLevelArchive(controller)) total++;
        return total;
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        int mounted = 0;
        for (FsController<?> controller : model)
            if (isTopLevelArchive(controller))
                if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    private boolean isTopLevelArchive(final FsController<?> controller) {
        final FsController<?> parent = controller.getParent();
        return null != parent && null == parent.getParent();
    }

    @Override
    public void sync() throws FsSyncException {
        model.sync(FsSyncOptions.NONE);
    }

    @Override
    public void clearStatistics() {
        JmxDirector.SINGLETON.clearStatistics();
    }
}
