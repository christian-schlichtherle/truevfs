/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides immutable maps of file system schemes to nullable drivers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface FsDriverMapProvider {

    /**
     * Returns an immutable map of file system schemes to nullable drivers.
     * Only the values of the returned map may be {@code null}!
     * <p>
     * Implementations are free to return the same instance (property method)
     * or a new instance (factory method) upon each call.
     * So clients may need to cache the result for future reuse.
     *
     * @return An immutable map of file system schemes to nullable drivers.
     */
    Map<FsScheme, FsDriver> apply();
}
