/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import java.util.Map;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.sl.FsDriverMapLocator;

/**
 * An abstract locatable service for creating maps of file system schemes to
 * file system drivers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsDriverMapLocator}.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsDriverMapFactory
extends ServiceProvider
implements FsDriverMapProvider {

    /**
     * Returns a new map of file system schemes to nullable drivers.
     * Note that only the values of the returned map may be {@code null}!
     *
     * @return A new map of file system schemes to nullable drivers.
     */
    @Override
    public abstract Map<FsScheme, FsDriver> drivers();
}
