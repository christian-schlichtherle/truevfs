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

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A factory for common output sockets.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the common entries.
 * @see     InputSocketFactory
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputSocketFactory<CE extends CommonEntry> {

    /**
     * Returns a new non-{@code null} output socket for write access to the
     * given local target.
     * <p>
     * When called on the returned common output socket, the method
     * {@link OutputSocket#getTarget()} <em>must</em> return an object which
     * {@link Object#equals(Object) compares equal} to the given local target
     * but is not necessarily the same.
     *
     * @param  entry the non-{@code null} local target.
     * @throws NullPointerException if {@code target} is {@code null}.
     * @throws CommonOuputBusyException if the local target is currently busy
     *         on output.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the local target is not busy anymore.
     * @throws FileNotFoundException if the local target is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new non-{@code null} output socket for writing to the local
     *         target.
     */
    OutputSocket<CE> newOutputSocket(CE entry) throws IOException;
}
