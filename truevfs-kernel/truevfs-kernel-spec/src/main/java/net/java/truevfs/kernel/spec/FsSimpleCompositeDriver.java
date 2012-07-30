/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;

/**
 * Uses a given file system driver provider to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsSimpleCompositeDriver extends FsAbstractCompositeDriver {

    private final Provider<Map<FsScheme, FsDriver>> provider;

    /**
     * Constructs a new simple composite driver which will query the given
     * {@code provider} for an appropriate file system driver for the scheme
     * of a given mount point.
     * 
     * @param provider the driver map provider.
     */
    public FsSimpleCompositeDriver(final Provider<Map<FsScheme, FsDriver>> provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return provider.get();
    }
}
