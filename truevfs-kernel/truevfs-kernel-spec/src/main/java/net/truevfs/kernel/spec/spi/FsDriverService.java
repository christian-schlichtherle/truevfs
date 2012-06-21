/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.truevfs.kernel.spec.FsAbstractDriverProvider;
import net.truevfs.kernel.spec.sl.FsDriverLocator;

/**
 * An abstract locatable service for a map of file system schemes to
 * file system drivers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsDriverLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsDriverService extends FsAbstractDriverProvider {

    /**
     * Returns a priority to help the file system driver service locator.
     * The greater number wins!
     * The default value should be zero.
     * 
     * @return A priority to help the file system driver service locator.
     */
    public abstract int getPriority();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d, drivers=%s]",
                getClass().getName(),
                getPriority(),
                getDrivers());
    }
}
