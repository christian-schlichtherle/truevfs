/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;

/**
 * The MXBean implementation for a {@linkplain FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManagerView 
extends StandardMBean implements JmxManagerMXBean {
    protected final JmxManagerController manager;

    public JmxManagerView(final JmxManagerController manager) {
        super(JmxManagerMXBean.class, true);
        this.manager = Objects.requireNonNull(manager);
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
        for (final FsController controller : manager)
            if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        int total = 0;
        for (final FsController controller : manager)
            if (isTopLevelArchive(controller)) total++;
        return total;
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        int mounted = 0;
        for (final FsController controller : manager)
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
        manager.sync();
    }

    @Override
    public void clearStatistics() {
        manager.clearStatistics();
    }
}
