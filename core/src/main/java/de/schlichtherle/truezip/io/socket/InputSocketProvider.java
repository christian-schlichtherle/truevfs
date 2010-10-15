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

package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.CommonEntry;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A provider for common input sockets.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <LT> the type of the
 *          {@link InputSocket#getLocalTarget() local target} for I/O
 *          operations.
 * @see     OutputSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputSocketProvider<LT extends CommonEntry> {

    /**
     * Returns a non-{@code null} input socket for read access to the given
     * <i>local target</i>.
     * <p>
     * When called on the returned input socket, the method
     * {@link InputSocket#getLocalTarget()} <em>must</em> return the same
     * object.
     *
     * @param  entry the non-{@code null} local target.
     * @throws NullPointerException if {@code target} is {@code null}.
     * @throws CommonInputBusyException if the local target is currently busy
     *         on input.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the local target is not busy anymore.
     * @throws FileNotFoundException if the local target does not exist or is
     *         not accessible for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A non-{@code null} input socket for reading from the local
     *         target.
     */
    InputSocket<? extends LT> getInputSocket(LT entry) throws IOException;
}
