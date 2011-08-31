/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket.spi;

import de.schlichtherle.truezip.socket.ByteArrayIOEntry;
import de.schlichtherle.truezip.socket.ByteArrayIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of a {@link ByteArrayIOPool byte array I/O pool}.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ByteArrayIOPoolService extends IOPoolService {

    // Don't make this static. Having multiple instances is good for debugging
    // the allocation and release of resources in a more isolated context.
    private final ByteArrayIOPool pool;

    /**
     * Constructs a new instance which provides a
     * {@link ByteArrayIOPool byte array I/O pool} where each allocated
     * {@link ByteArrayIOEntry byte array I/O entry} has an initial capacity
     * of the given number of bytes.
     * 
     * @param initialCapacity the initial capacity in bytes.
     */
    public ByteArrayIOPoolService(int initialCapacity) {
        pool = new ByteArrayIOPool(initialCapacity);
    }

    @Override
    public IOPool<?> get() {
        return pool;
    }
}
