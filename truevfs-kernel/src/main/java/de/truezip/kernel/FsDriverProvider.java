/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import java.util.Map;

/**
 * A provider for an immutable map of file system schemes to nullable drivers.
 *
 * @author Christian Schlichtherle
 */
public interface FsDriverProvider {

    /**
     * Returns an immutable map of file system schemes to nullable drivers.
     * Only the values of the returned map may be {@code null}.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     *
     * @return An immutable map of file system schemes to nullable drivers.
     */
    Map<FsScheme, FsDriver> getDrivers();
}
