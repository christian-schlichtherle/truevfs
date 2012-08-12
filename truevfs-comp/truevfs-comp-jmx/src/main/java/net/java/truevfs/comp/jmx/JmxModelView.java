/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.Entry.Access;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import net.java.truevfs.kernel.spec.cio.Entry.Entity;
import net.java.truevfs.kernel.spec.cio.Entry.Size;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import net.java.truevfs.kernel.spec.cio.Entry.Type;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * A view for a {@linkplain FsModel file system model}.
 *
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxModelView<M extends FsModel>
extends StandardMBean implements JmxModelMXBean {

    private static final FsMetaDriver
            DRIVER = new FsSimpleMetaDriver(FsDriverMapLocator.SINGLETON);

    protected final M model;

    public JmxModelView(M model) { this(JmxModelMXBean.class, model); }

    protected JmxModelView(
            final Class<? extends JmxModelMXBean> type,
            final M model) {
        super(type, true);
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
        return node().getSize(type);
    }

    @Override
    public String getTimeCreatedDate() {
        final long time = node().getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeCreatedMillis() {
        final long time = node().getTime(CREATE);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeReadDate() {
        final long time = node().getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeReadMillis() {
        final long time = node().getTime(READ);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeWrittenDate() {
        final long time = node().getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeWrittenMillis() {
        final long time = node().getTime(WRITE);
        return UNKNOWN == time ? null : time;
    }

    protected FsNode node() {
        final FsMountPoint mmp = model.getMountPoint();
        final FsMountPoint pmp = mmp.getParent();
        final FsMountPoint mp;
        final FsNodeName en;
        if (null != pmp) {
            mp = pmp;
            en = mmp.getPath().getNodeName();
        } else {
            mp = mmp;
            en = FsNodeName.ROOT;
        }
        FsNode node;
        try {
            node = FsManagerLocator
                    .SINGLETON
                    .get()
                    .controller(DRIVER, mp)
                    .node(FsAccessOptions.NONE, en);
        } catch (IOException ex) {
            node = null;
        }
        if (null != node) return node;
        return new FsNode() {
            @Override
            public String getName() {
                return en.toString();
            }

            @Override
            public BitField<Type> getTypes() {
                return Entry.NO_TYPES;
            }

            @Override
            public Set<String> getMembers() {
                return Collections.emptySet();
            }

            @Override
            public long getSize(Size type) {
                return UNKNOWN;
            }

            @Override
            public long getTime(Access type) {
                return UNKNOWN;
            }

            @Override
            public Boolean isPermitted(Access type, Entity entity) {
                return null;
            }
        };
    }

    @Override
    public void sync() throws FsSyncWarningException, FsSyncException {
        new FsFilteringManager( model.getMountPoint(),
                                FsManagerLocator.SINGLETON.get())
                .sync(FsSyncOptions.NONE);
    }
}
