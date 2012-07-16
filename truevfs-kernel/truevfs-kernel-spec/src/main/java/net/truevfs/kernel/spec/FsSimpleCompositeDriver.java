/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Uses a given file system driver provider to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsSimpleCompositeDriver extends FsAbstractCompositeDriver {

    private final FsDriverMapProvider provider;

    /**
     * Constructs a new simple composite driver which will query the given
     * driver {@code provider} for an appropriate file system driver for
     * the scheme of a given mount point.
     * 
     * @param provider the driver map provider.
     */
    public FsSimpleCompositeDriver(final FsDriverMapProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public Map<FsScheme, FsDriver> apply() {
        return provider.apply();
    }
}
