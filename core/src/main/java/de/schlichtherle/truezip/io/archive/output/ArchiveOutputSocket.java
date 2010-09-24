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

package de.schlichtherle.truezip.io.archive.output;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates output streams for writing bytes to its <i>local target</i>
 * archive entry.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the {@link #getTarget() local target} archive entry.
 * @see     ArchiveInputSocket
 * @see     ArchiveOutput
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class ArchiveOutputSocket<AE extends ArchiveEntry>
extends OutputSocket<AE, ArchiveEntry> {

    @Override
    public ArchiveOutputSocket<AE> chain(OutputSocket<AE, ArchiveEntry> output) {
        super.chain(output);
        return this;
    }

    @Override
    public ArchiveOutputSocket<AE> peer(
            InputSocket<? extends ArchiveEntry, ? super AE> peer) {
        super.peer(peer);
        return this;
    }

    /**
     * Returns the non-{@code null} local target archive entry.
     * <p>
     * Implementations must reflect any changes to the state of the returned
     * archive entry by the client applications before a call to the method
     * {@link #newOutputStream()}.
     * The effect of any subsequent changes to the state of the returned
     * archive entry is undefined.
     *
     * @return The non-{@code null} local archive entry target.
     */
    @Override
    public abstract AE getTarget();

    /**
     * {@inheritDoc}
     *
     * @throws OutputArchiveBusyException if the archive is currently busy
     *         on output for another entry.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the same entry again as soon as
     *         the archive is not busy on output anymore.
     * @throws FileNotFoundException if the archive entry is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     */
    @Override
    public abstract OutputStream newOutputStream() throws IOException;
}
