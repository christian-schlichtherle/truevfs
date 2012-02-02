/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * Uses a given file system driver service to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FsDefaultDriver extends FsAbstractCompositeDriver {

    private final Map<FsScheme, FsDriver> drivers;

    /**
     * Constructs a new file system default driver which will query the given
     * file system driver provider for an appropriate file system driver for
     * the scheme of a given mount point.
     */
    public FsDefaultDriver(final FsDriverProvider provider) {
        this.drivers = provider.get(); // dedicated immutable map!
        assert null != drivers;
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
