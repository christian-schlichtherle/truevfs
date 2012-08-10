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
import net.java.truevfs.comp.jmx.JmxManagerMXBean;
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
    protected final JmxManager manager;

    public JmxManagerView(final JmxManager manager) {
        super(JmxManagerMXBean.class, true);
        this.manager = Objects.requireNonNull(manager);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A file system manager.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "FileSystemsTotal":
            return "The total number of file systems.";
        case "FileSystemsMounted":
            return "The number of file systems which have been mounted.";
        case "TopLevelArchiveFileSystemsTotal":
            return "The total number of top level archive file systems.";
        case "TopLevelArchiveFileSystemsMounted":
            return "The number of top level archive file systems which have been mounted.";
        default:
            return null;
        }
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanOperationInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        switch (info.getName()) {
        case "sync":
            return "Synchronizes all file systems and eventually unmounts them.";
        default:
            return null;
        }
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
}
