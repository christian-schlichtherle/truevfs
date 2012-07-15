/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOptions;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;

/**
 * The pace manager view.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceManagerView extends StandardMBean implements PaceManager {

    private final PaceManagerModel model;
    private volatile PaceManagerController controller;

    PaceManagerView(final PaceManagerModel model) {
        super(PaceManager.class, true);
        this.model = model;
    }

    FsManager decorate(final FsManager manager) {
        assert !(manager instanceof PaceManagerController);
        final PaceManagerController o = controller;
        final PaceManagerModel m = null == o ? model : new PaceManagerModel();
        final PaceManagerController n = new PaceManagerController(m, manager);
        if (null == o) controller = n;
        return n;
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

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        return "A pace manager";
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanAttributeInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        final String name = info.getName();
        if (name.equals("FileSystemsMounted")) {
            return "The number of mounted file systems.";
        } else if (name.equals("FileSystemsTotal")) {
            return "The total number of file systems.";
        } else if (name.equals("MaximumFileSystemsMounted")) {
            return "The maximum number of mounted file systems.";
        } else if (name.equals("TopLevelArchiveFileSystemsMounted")) {
            return "The number of mounted top level archive file systems.";
        } else if (name.equals("TopLevelArchiveFileSystemsTotal")) {
            return "The total number of top level archive file systems.";
        }
        return null;
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
        if (info.getName().equals("sync"))
            return "Synchronizes all file systems.";
        return null;
    }

    @Override
    public void sync() throws FsSyncException {
        if (null != controller) controller.sync(FsSyncOptions.NONE);
    }

    @Override
    public int getFileSystemsTotal() {
        return null == controller ? 0 : controller.getSize();
    }

    @Override
    public int getFileSystemsMounted() {
        return model.getFileSystemsMounted();
    }

    @Override
    public int getMaximumFileSystemsMounted() {
        return model.getMaximumFileSystemsMounted();
    }

    @Override
    public void setMaximumFileSystemsMounted(int value) {
        model.setMaximumFileSystemsMounted(value);
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        int total = 0;
        if (null != controller)
            for (final FsController<?> c : controller)
                if (isTopLevelArchive(c)) total++;
        return total;
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        int mounted = 0;
        if (null != controller)
            for (final FsController<?> c : controller)
                if (isTopLevelArchive(c))
                    if (c.getModel().isMounted()) mounted++;
        return mounted;
    }

    private boolean isTopLevelArchive(final FsController<?> c) {
        final FsController<?> parent = c.getParent();
        return null != parent && null == parent.getParent();
    }
}
