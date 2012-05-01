/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.spi;

import de.truezip.kernel.cio.ByteArrayIOBuffer;
import de.truezip.kernel.cio.ByteArrayIOPool;
import de.truezip.kernel.cio.IOPool;
import javax.annotation.concurrent.Immutable;

/**
 * An immutable locatable container of a {@link ByteArrayIOPool byte array I/O buffer pool}.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class ByteArrayIOPoolService extends IOPoolService {

    // Don't make this static. Having multiple instances is good for debugging
    // the allocation and release of resources in a more isolated context.
    private final ByteArrayIOPool pool;

    /**
     * Constructs a new instance which provides a
     * {@link ByteArrayIOPool byte array I/O pool} where each allocated
     * {@link ByteArrayIOBuffer byte array I/O entry} has an initial capacity
     * of the given number of bytes.
     * 
     * @param initialCapacity the initial capacity in bytes.
     */
    public ByteArrayIOPoolService(final int initialCapacity) {
        pool = new ByteArrayIOPool(initialCapacity);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public IOPool<?> getIOPool() {
        return pool;
    }
}