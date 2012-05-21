/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spi;

import net.truevfs.kernel.cio.ByteArrayIoBuffer;
import net.truevfs.kernel.cio.ByteArrayIoPool;
import net.truevfs.kernel.cio.IoPool;
import javax.annotation.concurrent.Immutable;

/**
 * An immutable locatable container of a {@link ByteArrayIoPool byte array I/O buffer pool}.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class ByteArrayIoPoolService extends IoPoolService {

    // Don't make this static. Having multiple instances is good for debugging
    // the allocation and release of resources in a more isolated context.
    private final ByteArrayIoPool pool;

    /**
     * Constructs a new instance which provides a
     * {@link ByteArrayIoPool byte array I/O pool} where each allocated
     * {@link ByteArrayIoBuffer byte array I/O entry} has an initial capacity
     * of the given number of bytes.
     * 
     * @param initialCapacity the initial capacity in bytes.
     */
    public ByteArrayIoPoolService(final int initialCapacity) {
        pool = new ByteArrayIoPool(initialCapacity);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public IoPool<?> getIoPool() {
        return pool;
    }
}