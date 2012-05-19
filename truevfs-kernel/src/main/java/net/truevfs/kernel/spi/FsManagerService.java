/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spi;

import net.truevfs.kernel.FsManagerProvider;
import net.truevfs.kernel.sl.FsManagerLocator;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract locatable service for a file system manager.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsManagerService implements FsManagerProvider {

    /**
     * Returns a priority to help the file system manager service locator.
     * The greater number wins!
     * The default value should be zero.
     * 
     * @return A priority to help the file system manager service locator.
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
