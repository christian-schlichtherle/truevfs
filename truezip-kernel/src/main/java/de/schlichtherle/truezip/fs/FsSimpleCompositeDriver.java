/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * Uses a given file system driver service to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author  Christian Schlichtherle
 */
@Immutable
public final class FsSimpleCompositeDriver extends FsAbstractCompositeDriver {

    private final Map<FsScheme, FsDriver> drivers;

    /**
     * Constructs a new file system default driver which will query the given
     * file system driver provider for an appropriate file system driver for
     * the scheme of a given mount point.
     * 
     * @param provider the driver provider.
     */
    public FsSimpleCompositeDriver(final FsDriverProvider provider) {
        this.drivers = provider.get(); // dedicated immutable map!
        assert null != drivers;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
