/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.shed.Filter;
import global.namespace.truevfs.comp.shed.Visitor;

import java.util.Objects;

/**
 * A filter which accepts a given file system
 * {@linkplain FsController controller} if its file system
 * {@linkplain FsModel model} is accepted by the configured file system model
 * {@linkplain Filter filter}.
 *
 * @see    FsManager#accept(Filter, Visitor)
 * @author Christian Schlichtherle
 */
public final class FsControllerFilter implements Filter<FsController> {

    private final Filter<? super FsModel> filter;

    public FsControllerFilter(final Filter<? super FsModel> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    /** Returns a controller filter for the given prefix mount point. */
    public static FsControllerFilter forPrefix(FsMountPoint prefix) {
        return new FsControllerFilter(FsModelFilter.forPrefix(prefix));
    }

    @Override
    public boolean accept(FsController controller) {
        return filter.accept(controller.getModel());
    }
}
