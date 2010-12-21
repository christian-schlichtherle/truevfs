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
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * Implements a caching strategy for input sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the local
 *     target and temporarily stored in this cache.
 *     Subsequent or concurrent read operations will be served from this cache
 *     without re-reading the data from the local target again until this cache
 *     gets {@link InputCache#clear cleared}.</li>
 * </ul>
 *
 * @see     OutputCache
 * @see     IOCache
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputCache<LT extends Entry> {

    /** Returns an input socket for cached read operations. */
    @NonNull InputSocket<LT> getInputSocket();

    /**
     * Clears this cache and triggers re-reading the data from the local target
     * upon the next read operation.
     */
    void clear() throws IOException;
}
