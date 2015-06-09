/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.Filter;

import java.util.Objects;

/**
 * A filter which accepts a given
 * {@linkplain FsController file system controller} if its
 * {@linkplain FsModel file system model} is accepted by the configured
 * file system model {@linkplain Filter filter}.
 *
 * @see    FsManager#sync
 * @author Christian Schlichtherle
 */
public final class FsControllerFilter implements Filter<FsController> {

    private final Filter<? super FsModel> filter;

    public FsControllerFilter(FsMountPoint prefix) {
        this(new FsModelFilter(prefix));
    }

    public FsControllerFilter(final Filter<? super FsModel> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public boolean accept(FsController controller) {
        return filter.accept(controller.getModel());
    }
}
