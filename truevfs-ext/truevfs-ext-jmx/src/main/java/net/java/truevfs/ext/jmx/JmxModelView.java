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
 * The MXBean view for a {@linkplain FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxModelView
extends StandardMBean implements JmxModelMXBean {

    protected final JmxModel model;

    public JmxModelView(final JmxModel model) {
        super(JmxModelMXBean.class, true);
        this.model = Objects.requireNonNull(model);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A file system model.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "Mounted":
            return "Whether or not this file system is mounted.";
        case "MountPoint":
            return "The mount point URI of this file system.";
        case "MountPointOfParent":
            return "The mount point URI of the parent file system.";
        case "SizeOfData":
            return "The data size of this file system.";
        case "SizeOfStorage":
            return "The storage size of this file system.";
        case "TimeCreatedDate":
            return "The time this file system has been created.";
        case "TimeCreatedMillis":
            return "The time this file system has been created in milliseconds.";
        case "TimeReadDate":
            return "The last time this file system has been read or accessed.";
        case "TimeReadMillis":
            return "The last time this file system has been read or accessed in milliseconds.";
        case "TimeWrittenDate":
            return "The last time this file system has been written.";
        case "TimeWrittenMillis":
            return "The last time this file system has been written in milliseconds.";
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
            return "Synchronizes this file system and all enclosed file systems and eventually unmounts them.";
        default:
            return null;
        }
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
    public String getTimeCreatedDate() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeCreatedMillis() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(CREATE);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeReadDate() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeReadMillis() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(READ);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeWrittenDate() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeWrittenMillis() {
        final FsNode node = model.stat();
        final long time = null == node ? UNKNOWN : node.getTime(WRITE);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public void sync() throws FsSyncException {
        model.sync();
    }
}
