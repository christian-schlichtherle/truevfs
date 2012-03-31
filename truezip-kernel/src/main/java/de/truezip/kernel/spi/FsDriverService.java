/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.spi;

import de.truezip.kernel.FsAbstractDriverProvider;
import de.truezip.kernel.sl.FsDriverLocator;

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
}
