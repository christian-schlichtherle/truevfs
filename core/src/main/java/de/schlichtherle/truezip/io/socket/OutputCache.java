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

import de.schlichtherle.truezip.io.entry.Entry;
import java.io.IOException;

/**
 * Implements a caching strategy for output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>At the discretion of the implementation, data written to this cache may
 *     not be written to the local target until this cache gets
 *     {@link OutputCache#flush flushed}.</li>
 * </ul>
 *
 * @see     InputCache
 * @see     IOCache
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputCache<LT extends Entry> {

    /** Returns an output socket for cached write operations. */
    OutputSocket<LT> getOutputSocket();

    /**
     * Ensures that the last data written to this cache is written to the local
     * target, too.
     */
    void flush() throws IOException;

    /**
     * Clears this cache, effectively discarding any written data which has not
     * been {@link #flush() flushed} to the local target before.
     */
    void clear() throws IOException;
}
