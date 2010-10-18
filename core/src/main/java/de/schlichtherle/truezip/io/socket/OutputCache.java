/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.CommonEntry;
import java.io.IOException;

/**
 * Implements a caching strategy for output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Any data written to the cache will get written to the local target if
 *     and only if the cache gets flushed.
 * </ul>
 *
 * @see     InputCache
 * @see     Cache
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputCache<LT extends CommonEntry> {

    /** Returns an output socket for cached write operations. */
    OutputSocket<LT> getOutputSocket() throws IOException;

    /**
     * Writes the data in the cache to the underlying storage.
     */
    void flush() throws IOException;

    /**
     * Clears the cache, effectively throwing away any data which has not been
     * {@link #flush() flushed} to the underlying storage.
     */
    void clear() throws IOException;
}
