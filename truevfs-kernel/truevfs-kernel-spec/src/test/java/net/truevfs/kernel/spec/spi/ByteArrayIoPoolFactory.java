/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.ByteArrayIoBuffer;
import net.truevfs.kernel.spec.cio.ByteArrayIoPool;

/**
 * Creates {@linkplain ByteArrayIoPool byte array I/O pools}.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class ByteArrayIoPoolFactory extends IoPoolFactory {

    private final int initialCapacity;

    /**
     * Constructs a new instance which creates byte array I/O pools where each
     * allocated {@link ByteArrayIoBuffer byte array I/O buffer} has an initial
     * capacity of the given number of bytes.
     * 
     * @param initialCapacity the initial capacity in bytes.
     */
    public ByteArrayIoPoolFactory(final int initialCapacity) {
        if (0 > (this.initialCapacity = initialCapacity))
            throw new IllegalArgumentException();
    }

    @Override
    public ByteArrayIoPool ioPool() {
        return new ByteArrayIoPool(initialCapacity);
    }
}
