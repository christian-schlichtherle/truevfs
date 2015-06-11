/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSync;
import net.java.truevfs.kernel.spec.FsSyncException;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A view for a {@linkplain FsManager file system manager}.
 *
 * @param  <M> the type of the file system manager.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManagerView<M extends FsManager>
extends StandardMBean implements JmxManagerMXBean {

    protected final M manager;

    public JmxManagerView(M manager) { this(JmxManagerMXBean.class, manager); }

    protected JmxManagerView(
            final Class<? extends JmxManagerMXBean> type,
            final M manager) {
        super(type, true);
        this.manager = Objects.requireNonNull(manager);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A file system manager.";
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "FileSystemsMounted":
            return "The number of file systems which have been mounted.";
        case "FileSystemsTotal":
            return "The total number of file systems.";
        case "TopLevelArchiveFileSystemsMounted":
            return "The number of top level archive file systems which have been mounted.";
        case "TopLevelArchiveFileSystemsTotal":
            return "The total number of top level archive file systems.";
        default:
            return null;
        }
    }

    @Override
    protected String getDescription(final MBeanOperationInfo info) {
        switch (info.getName()) {
        case "sync":
            return "Synchronizes all file systems and eventually unmounts them.";
        default:
            return null;
        }
    }

    @Override
    public int getFileSystemsTotal() {
        return count(Filter.ACCEPT_ANY);
    }

    @Override
    public int getFileSystemsMounted() {
        return count(new MountedFileSystemsFilter());
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        return count(new TotalTopLevelArchiveFileSystemsFilter());
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        return count(new MountedTopLevelArchiveFileSystemsFilter());
    }

    private int count(Filter<? super FsController> filter) {
        return manager.accept(filter, new CountingVisitor()).get();
    }

    @Override
    public void sync() throws FsSyncException {
        new FsSync().run();
    }
}

final class MountedFileSystemsFilter
implements Filter<FsController> {

    @Override
    public boolean accept(FsController controller) {
        return controller.getModel().isMounted();
    }
}

final class TotalTopLevelArchiveFileSystemsFilter
implements Filter<FsController> {

    @Override
    public boolean accept(final FsController controller) {
        final FsController parent = controller.getParent();
        return null != parent && null == parent.getParent();
    }
}

final class MountedTopLevelArchiveFileSystemsFilter
implements Filter<FsController> {

    final Filter<FsController> mountedFileSystemsFilter = new MountedFileSystemsFilter();
    final Filter<FsController> totalTopLevelArchiveFileSystemsFilter = new TotalTopLevelArchiveFileSystemsFilter();

    @Override
    public boolean accept(FsController controller) {
        return mountedFileSystemsFilter.accept(controller) &&
                totalTopLevelArchiveFileSystemsFilter.accept(controller);
    }
}

final class CountingVisitor
extends AtomicInteger implements Visitor<FsController, RuntimeException> {

    @Override
    public void visit(FsController controller) { incrementAndGet(); }
}
