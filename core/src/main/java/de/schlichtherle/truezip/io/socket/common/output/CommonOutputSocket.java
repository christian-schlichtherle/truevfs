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
package de.schlichtherle.truezip.io.socket.common.output;

import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.common.CommonEntry;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates output streams for writing bytes to its <i>local target</i>
 * common entry.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the {@link #getTarget() local target} common entry.
 * @see     CommonInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class CommonOutputSocket<CE extends CommonEntry>
extends OutputSocket<CE, CommonEntry> {

    @Override
    public CommonOutputSocket<CE> chain(OutputSocket<CE, CommonEntry> output) {
        super.chain(output);
        return this;
    }

    @Override
    public CommonOutputSocket<CE> peer(
            InputSocket<? extends CommonEntry, ? super CE> peer) {
        super.peer(peer);
        return this;
    }

    /**
     * Returns the non-{@code null} local target common entry.
     * <p>
     * Implementations must reflect any changes to the state of the returned
     * common entry by the client applications before a call to the method
     * {@link #newOutputStream()}.
     * The effect of any subsequent changes to the state of the returned
     * common entry is undefined.
     *
     * @return The non-{@code null} local common entry target.
     */
    @Override
    public abstract CE getTarget();

    /**
     * {@inheritDoc}
     *
     * @throws CommonOuputBusyException if the socket's destination is
     *         currently busy with output.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the socket's destination is not busy anymore.
     * @throws FileNotFoundException if the common entry is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     */
    @Override
    public abstract OutputStream newOutputStream() throws IOException;
}
