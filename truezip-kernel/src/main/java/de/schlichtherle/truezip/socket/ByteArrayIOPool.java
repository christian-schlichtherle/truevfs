/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * A pool of byte array I/O entries.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class ByteArrayIOPool implements IOPool<ByteArrayIOEntry> {

    private static final String MOCK_ENTRY_NAME = "mock";

    private final int initialCapacity;
    private volatile int total;
    private volatile int active;

    /** Equivalent to {@link #ByteArrayIOPool(int) new ByteArrayIOPool(32)}. */
    public ByteArrayIOPool() {
        this(32);
    }

    /**
     * Constructs a new byte array I/O pool.
     *
     * @param initialCapacity the initial capacity of the array to use for
     *        writing to an allocated I/O entry.
     */
    public ByteArrayIOPool(final int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Negative initial capacity: " + initialCapacity);
        this.initialCapacity = initialCapacity;
    }

    @Override
    public synchronized Entry allocate() {
        Entry entry = new Entry(total++);
        active++;
        return entry;
    }

    @Override
    public void release(IOPool.Entry<ByteArrayIOEntry> entry) throws IOException {
        entry.release();
    }

    /**
     * Returns the number of I/O entries allocated but not yet released from
     * this pool.
     *
     * @return The number of I/O entries allocated but not yet released from
     *         this pool.
     */
    public int getSize() {
        return active;
    }

    @NotThreadSafe
    public final class Entry
    extends ByteArrayIOEntry
    implements IOPool.Entry<ByteArrayIOEntry> {
        private boolean released;

        private Entry(int i) {
            super(MOCK_ENTRY_NAME + i, ByteArrayIOPool.this.initialCapacity);
        }

        @Override
        public void release() throws IOException {
            if (released)
                throw new IllegalStateException("entry has already been released!");
            released = true;
            synchronized (ByteArrayIOPool.this) {
                active--;
            }
            setData(null);
        }
    }
}
