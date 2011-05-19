/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A service for input sockets.
 *
 * @param   <E> The type of the entries.
 * @see     OutputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputService<E extends Entry> extends EntryContainer<E> {

    /**
     * Returns an input socket for read access to the given entry.
     *
     * @param  name a non-{@code null} {@link Entry#getName() entry name}.
     * @return An input socket for reading from the local target.
     */
    @NonNull InputSocket<? extends E> getInputSocket(@NonNull String name);
}
