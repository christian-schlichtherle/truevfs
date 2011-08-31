/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
