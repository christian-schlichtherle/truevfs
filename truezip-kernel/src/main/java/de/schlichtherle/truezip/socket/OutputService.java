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
import net.jcip.annotations.NotThreadSafe;

/**
 * A service for output sockets.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have only been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> The type of the entries.
 * @see     InputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputService<E extends Entry> extends EntryContainer<E> {

    /**
     * Returns an output socket for write access to the given entry.
     *
     * @param  entry the non-{@code null} local target.
     * @return An output socket for writing to the local target.
     */
    @NonNull OutputSocket<? extends E> getOutputSocket(@NonNull E entry);
}
