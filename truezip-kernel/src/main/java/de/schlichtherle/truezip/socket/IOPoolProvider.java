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
     * Returns an I/O entry pool.
     * <p>
     * Calling this method several times may return different I/O entry pools,
     * so callers should cache the result for subsequent use.
     *
     * @return An I/O entry pool.
     */
    IOPool<?> get();
}
