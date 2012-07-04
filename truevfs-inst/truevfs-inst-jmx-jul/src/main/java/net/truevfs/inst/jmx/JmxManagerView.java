/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.inst.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.*;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsSyncException;
import net.truevfs.kernel.spec.FsSyncOption;
import static net.truevfs.kernel.spec.FsSyncOption.CLEAR_CACHE;
import net.truevfs.kernel.spec.util.BitField;

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
    private static final BitField<FsSyncOption>
            SYNC_OPTIONS = BitField.of(CLEAR_CACHE);

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
        try {
            return new ObjectName(  FsManager.class.getName(),
                                    "name",
                                    "SINGLETON");
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
        switch (info.getName()) {
        case "sync":
            description = "Synchronizes all managed federated file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
            break;
        case "clearStatistics":
            description = "Clears all but the last I/O statistics.";
            break;
        }
        return description;
    }

    @Override
    public JmxModelViewMXBean[] getFederatedFileSystems() {
        int size = model.size();
        List<JmxModelViewMXBean> list = new ArrayList<>(size);
        for (FsController<?> controller : model)
            list.add(JmxModelView.register(controller.getModel()));
        return list.toArray(new JmxModelViewMXBean[size]);
    }

    @Override
    public int getFileSystemsTotal() {
        return model.size();
    }

    @Override
    public int getFileSystemsTouched() {
        int result = 0;
        for (FsController<?> controller : model)
            if (controller.getModel().isTouched())
                result++;
        return result;
    }

    @Override
    public int getTopLevelFileSystemsTotal() {
        int result = 0;
        for (FsController<?> controller : model)
            if (null == controller.getParent().getParent())
                result++;
        return result;
    }

    @Override
    public int getTopLevelFileSystemsTouched() {
        int result = 0;
        for (FsController<?> controller : model) {
            if (null == controller.getParent().getParent())
                if (controller.getModel().isTouched())
                    result++;
        }
        return result;
    }

    @Override
    public void sync() throws FsSyncException {
        model.sync(SYNC_OPTIONS);
    }

    @Override
    public void clearStatistics() {
        JmxDirector.SINGLETON.clearStatistics();
    }
}