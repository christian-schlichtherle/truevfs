/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truevfs.kernel.spec.FsAbstractModel;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsMountPoint;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class DefaultModel extends FsAbstractModel {

    private volatile boolean mounted;

    DefaultModel(FsMountPoint mountPoint, Optional<? extends FsModel> parent) {
        super(mountPoint, parent);
    }

    @Override
    public boolean isMounted() {
        return mounted;
    }

    @Override
    public void setMounted(final boolean mounted) {
        this.mounted = mounted;
    }
}
