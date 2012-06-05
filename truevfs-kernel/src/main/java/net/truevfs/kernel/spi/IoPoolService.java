/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spi;

import net.truevfs.kernel.cio.IoPoolProvider;
import net.truevfs.kernel.sl.IoPoolLocator;

/**
 * An abstract locatable service for an I/O buffer pool.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IoPoolLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class IoPoolService implements IoPoolProvider {

    /**
     * Returns a priority to help the I/O pool service locator.
     * The greater number wins!
     * The default value should be zero.
     * 
     * @return A priority to help the I/O pool service locator.
     */
    public abstract int getPriority();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}