/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.entry.EntryContainer;
import de.schlichtherle.truezip.io.entry.Entry;
import net.jcip.annotations.NotThreadSafe;

/**
 * A container and input socket factory for entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <E> The type of the entries.
 * @see     OutputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public interface InputService<E extends Entry>
extends EntryContainer<E> {

    /**
     * Returns a non-{@code null} input socket for read access to the given
     * entry.
     *
     * @param  name a non-{@code null} {@link Entry#getName() entry name}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @return A non-{@code null} input socket for reading from the local
     *         target.
     */
    InputSocket<? extends E> getInputSocket(String name);
}
