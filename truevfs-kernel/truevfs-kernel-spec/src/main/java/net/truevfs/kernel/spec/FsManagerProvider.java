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
     *
     * @return A file system manager.
     */
    FsManager manager();
}
