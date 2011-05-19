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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An entry which provides input sockets.
 *
 * @see     OutputEntry
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputEntry<E extends InputEntry<E>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    @NonNull InputSocket<E> getInputSocket();
}
