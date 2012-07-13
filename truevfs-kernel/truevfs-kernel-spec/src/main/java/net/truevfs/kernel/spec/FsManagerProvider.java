/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides file system managers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface FsManagerProvider {

    /**
     * Returns a file system manager.
     * <p>
     * Implementations are free to return the same instance (property method)
     * or a new instance (factory method) upon each call.
     * So clients may need to cache the result for future reuse.
     *
     * @return A file system manager.
     */
    FsManager manager();
}
