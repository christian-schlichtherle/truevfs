/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

/**
 * A provider for an I/O buffer pool.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface IOPoolProvider {

    /**
     * Returns the I/O buffer pool to use for allocating temporary I/O buffers.
     * <p>
     * Multiple invocations should return the same I/O buffer pool.
     * However, callers should cache the return value for subsequent use in
     * case it isn't always the same.
     *
     * @return The I/O buffer pool to use for allocating temporary I/O buffers.
     */
    IOPool<?> get();
}
