/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

/**
 * A provider for an I/O buffer pool.
 *
 * @author Christian Schlichtherle
 */
public interface IOPoolProvider {

    /**
     * Returns the pool to use for allocating temporary I/O buffers.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     *
     * @return The pool to use for allocating temporary I/O buffers.
     */
    IOPool<?> getIOPool();
}
