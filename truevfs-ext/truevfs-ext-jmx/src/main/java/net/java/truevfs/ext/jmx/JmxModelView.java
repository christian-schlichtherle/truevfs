/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truevfs.comp.jmx.JmxModelMXBean;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsNode;
import net.java.truevfs.kernel.spec.FsSyncException;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import net.java.truevfs.kernel.spec.cio.Entry.Size;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;

/**
 * The MXBean implementation for a {@linkplain FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxModelView
extends StandardMBean implements JmxModelMXBean {
    protected final JmxModelController model;

    public JmxModelView(final JmxModelController model) {
        super(JmxModelMXBean.class, true);
        this.model = Objects.requireNonNull(model);
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
     * You can supply a customized description for MBeanOperationInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        String description = null;
        if (info.getName().equals("sync"))
            description = "Synchronizes this file system and all enclosed file systems. If any file system is busy with I/O, an FsSyncException is thrown.";
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
        final FsNode node = model.stat();
        return null == node ? UNKNOWN : node.getSize(type);
    }

    @Override
    public String getTimeWritten() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public void sync() throws FsSyncException {
        model.sync();
    }
}
