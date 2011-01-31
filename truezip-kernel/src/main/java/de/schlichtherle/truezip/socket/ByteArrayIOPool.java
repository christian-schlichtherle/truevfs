/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
