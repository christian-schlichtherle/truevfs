/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import java.util.Map;

/**
 * A provider for an immutable map of file system schemes to drivers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface FsDriverProvider {

    /**
     * Returns an immutable map of file system schemes to drivers.
     * Neither the keys nor the values of the returned map may be {@code null}.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     *
     * @return A dedicated immutable map of file system schemes to drivers.
     */
    Map<FsScheme, FsDriver> getDrivers();
}
