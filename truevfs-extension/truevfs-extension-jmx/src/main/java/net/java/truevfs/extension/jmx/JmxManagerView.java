/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOptions;

/**
 * The MXBean implementation for a {@linkplain FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxManagerView
extends StandardMBean implements JmxManagerMXBean {
    private final FsManager manager;

    static void register(final FsManager model) {
        final JmxManagerMXBean mbean = new JmxManagerView(model);
        final ObjectName name = getObjectName(model);
        JmxUtils.registerMBean(mbean, name);
    }

    static void unregister(final FsManager model) {
        final ObjectName name = getObjectName(model);
        JmxUtils.unregisterMBean(name);
    }

    private static ObjectName getObjectName(final FsManager model) {
        try {
            return new ObjectName(
                    JmxManagerView.class.getPackage().getName(),
                    "type", FsManager.class.getSimpleName());
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    private JmxManagerView(final FsManager manager) {
        super(JmxManagerMXBean.class, true);
        this.manager = manager;
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A JMX file system manager.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        switch (info.getName()) {
        case "FederatedFileSystems":
            description = "The federated file systems managed by this instance.";
            break;
        case "FileSystemsTotal":
            description = "The total number of managed federated file systems.";
            break;
        case "FileSystemsTouched":
            description = "The number of managed federated file systems which have been touched and need unmounting.";
            break;
        case "TopLevelFileSystemsTotal":
            description = "The total number of managed top level federated file systems.";
            break;
        case "TopLevelFileSystemsTouched":
            description = "The number of managed top level federated file systems which have been touched and need unmounting.";
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
        switch (info.getName()) {
        case "sync":
            description = "Synchronizes all managed archive file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
            break;
        case "clearStatistics":
            description = "Clears all but the last I/O statistics.";
            break;
        }
        return description;
    }

    @Override
    public int getFileSystemsTotal() {
        return manager.size();
    }

    @Override
    public int getFileSystemsMounted() {
        int mounted = 0;
        for (FsController controller : manager)
            if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        int total = 0;
        for (FsController controller : manager)
            if (isTopLevelArchive(controller)) total++;
        return total;
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        int mounted = 0;
        for (FsController controller : manager)
            if (isTopLevelArchive(controller))
                if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    private boolean isTopLevelArchive(final FsController controller) {
        final FsController parent = controller.getParent();
        return null != parent && null == parent.getParent();
    }

    @Override
    public void sync() throws FsSyncException {
        manager.sync(FsSyncOptions.NONE);
    }

    @Override
    public void clearStatistics() {
        JmxDirector.SINGLETON.clearStatistics();
    }
}
