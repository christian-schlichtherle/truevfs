/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.Filter;

import java.util.Objects;

/**
 * A filter which accepts a given {@linkplain FsModel file system model} if its
 * {@linkplain FsMountPoint file system mount point} is accepted by the
 * configured file system mount point {@linkplain Filter filter}.
 *
 * @author Christian Schlichtherle
 */
public final class FsModelFilter implements Filter<FsModel> {

    private final Filter<? super FsMountPoint> filter;

    public FsModelFilter(FsMountPoint prefix) {
        this(new FsMountPointFilter(prefix));
    }

    public FsModelFilter(final Filter<? super FsMountPoint> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public boolean accept(FsModel model) {
        return filter.accept(model.getMountPoint());
    }
}
