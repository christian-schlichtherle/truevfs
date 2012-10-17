/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import net.java.truecommons.shed.ExceptionBuilder;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.SuppressedExceptionBuilder;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsControllerVisitor;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSimpleControllerSyncVisitor;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOptions;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

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
        class FileSystemsMounted implements Filter<FsController> {
            @Override
            public boolean accept(FsController controller) {
                return controller.getModel().isMounted();
            }
        }
        return count(new FileSystemsMounted());
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        class TopLevelArchiveFileSystemsTotal implements Filter<FsController> {
            @Override
            public boolean accept(FsController controller) {
                return isTopLevelArchive(controller);
            }
        }
        return count(new TopLevelArchiveFileSystemsTotal());
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        class TopLevelArchiveFileSystemsMounted implements Filter<FsController> {
            @Override
            public boolean accept(FsController controller) {
                return isTopLevelArchive(controller)
                        && controller.getModel().isMounted();
            }
        }
        return count(new TopLevelArchiveFileSystemsMounted());
    }

    private int count(final Filter<? super FsController> filter) {

        @ThreadSafe
        final class Visitor
        extends AtomicInteger implements FsControllerVisitor<IOException> {
            @Override
            public Filter<? super FsController> filter() { return filter; }

            @Override
            public SuppressedExceptionBuilder<IOException> builder() {
                return new SuppressedExceptionBuilder<>();
            }

            @Override
            public void visit(FsController controller) { incrementAndGet(); }
        } // Visitor

        final Visitor visitor = new Visitor();
        try {
            manager.visit(visitor);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
        return visitor.get();
    }

    private boolean isTopLevelArchive(final FsController controller) {
        final FsController parent = controller.getParent();
        return null != parent && null == parent.getParent();
    }

    @Override
    public void sync() throws FsSyncException {
        FsManagerLocator.SINGLETON.get().sync(
                new FsSimpleControllerSyncVisitor(FsSyncOptions.NONE));
    }
}
