/*
 * Copyright 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket.output;

import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.input.CommonInputProvider;
import java.io.IOException;

/**
 * Provides {@link CommonOutputSocket}s for write access to common entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the common entries.
 * @see     CommonInputProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface CommonOutputProvider<CE extends CommonEntry> {

    /**
     * Returns a new non-{@code null} output socket for write access to the
     * given local target.
     * <p>
     * When called on the returned common output socket, the method
     * {@link OutputSocket#getTarget()} <em>must</em> return an object which
     * {@link Object#equals(Object) compares equal} to the given local target
     * but is not necessarily the same.
     *
     * @param  target the non-{@code null} local target.
     * @return A new non-{@code null} output socket for writing to the local
     *         target.
     * @throws IOException If the local target
     *         is not accessible for some reason.
     * @throws NullPointerException if {@code target} is {@code null}.
     */
    CommonOutputSocket<CE> newOutputSocket(CE target) throws IOException;
}
