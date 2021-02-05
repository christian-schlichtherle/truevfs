/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.jmx;

import global.namespace.truevfs.commons.shed.Filter;
import global.namespace.truevfs.commons.shed.Visitor;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsSync;
import global.namespace.truevfs.kernel.api.FsSyncException;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A view for a {@linkplain FsManager file system manager}.
 *
 * @param <M> the type of the file system manager.
 * @author Christian Schlichtherle
 */
public class JmxManagerView<M extends FsManager> extends StandardMBean implements JmxManagerMXBean {

    protected final M manager;

    public JmxManagerView(M manager) {
        this(manager, JmxManagerMXBean.class);
    }

    protected JmxManagerView(final M manager, final Class<? extends JmxManagerMXBean> type) {
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
    protected String getDescription(MBeanOperationInfo info) {
        return "sync".equals(info.getName()) ? "Synchronizes all file systems and eventually unmounts them." : null;
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
        final Optional<? extends FsController> parent = controller.getParent();
        return parent.filter(p -> !p.getParent().isPresent()).isPresent();
    }
}

final class MountedTopLevelArchiveFileSystemsFilter implements Filter<FsController> {

    private final Filter<FsController> mountedFileSystemsFilter = new MountedFileSystemsFilter();

    private final Filter<FsController> totalTopLevelArchiveFileSystemsFilter =
            new TotalTopLevelArchiveFileSystemsFilter();

    @Override
    public boolean accept(FsController controller) {
        return mountedFileSystemsFilter.accept(controller) &&
                totalTopLevelArchiveFileSystemsFilter.accept(controller);
    }
}

final class CountingVisitor implements Visitor<FsController, RuntimeException> {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public void visit(FsController controller) {
        count.incrementAndGet();
    }

    int get() {
        return count.get();
    }
}
