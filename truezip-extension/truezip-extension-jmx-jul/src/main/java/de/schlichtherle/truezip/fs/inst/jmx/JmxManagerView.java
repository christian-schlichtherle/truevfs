/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.CLEAR_CACHE;
import de.schlichtherle.truezip.util.BitField;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.*;

/**
 * The MXBean implementation for a {@link FsManager file system manager}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
        if (info.getName().equals("FederatedFileSystems")) {
            description = "The federated file systems managed by this instance.";
        } else if (info.getName().equals("FileSystemsTotal")) {
            description = "The total number of managed federated file systems.";
        } else if (info.getName().equals("FileSystemsTouched")) {
            description = "The number of managed federated file systems which have been touched and need unmounting.";
        } else if (info.getName().equals("TopLevelFileSystemsTotal")) {
            description = "The total number of managed top level federated file systems.";
        } else if (info.getName().equals("TopLevelFileSystemsTouched")) {
            description = "The number of managed top level federated file systems which have been touched and need unmounting.";
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
        if (info.getName().equals("clearStatistics")) {
            description = "Clears all but the last I/O statistics.";
        } else if (info.getName().equals("umount")) {
            description = "Unmounts all managed federated file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
        }
        return description;
    }

    @Override
    public JmxModelViewMXBean[] getFederatedFileSystems() {
        int size = model.getSize();
        List<JmxModelViewMXBean> list = new ArrayList<JmxModelViewMXBean>(size);
        for (FsController<?> controller : model)
            list.add(JmxModelView.register(controller.getModel()));
        return list.toArray(new JmxModelViewMXBean[size]);
    }

    @Override
    public int getFileSystemsTotal() {
        return model.getSize();
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
    public void umount() throws FsSyncException {
        model.sync(SYNC_OPTIONS);
    }

    @Override
    public void clearStatistics() {
        JmxDirector.SINGLETON.clearStatistics();
    }
}
