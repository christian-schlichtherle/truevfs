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

package de.schlichtherle.truezip.io.archive.input;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams for reading bytes from its <i>local target</i>
 * archive entry.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the {@link #getTarget() local target} archive entry.
 * @see     ArchiveOutputSocket
 * @see     ArchiveInput
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class ArchiveInputSocket<AE extends ArchiveEntry>
extends InputSocket<AE, ArchiveEntry> {

    @Override
    public ArchiveInputSocket<AE> chain(
            InputSocket<? super AE, ? extends ArchiveEntry> output) {
        super.chain(output);
        return this;
    }

    @Override
    public ArchiveInputSocket<AE> peer(
            OutputSocket<? extends ArchiveEntry, ? super AE> newPeer) {
        super.peer(newPeer);
        return this;
    }

    /**
     * Returns the non-{@code null} local target archive entry.
     * <p>
     * Client applications must not change the state of the returned archive
     * entry.
     *
     * @return The non-{@code null} local archive entry target.
     */
    @Override
    public abstract AE getTarget();

    /**
     * {@inheritDoc}
     *
     * @throws InputArchiveBusyException if the archive is currently busy
     *         on input for another entry.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to read the same entry again as soon as
     *         the archive is not busy on input anymore.
     * @throws FileNotFoundException if the archive entry does not exist or
     *         is not accessible for some reason.
     * @throws IOException on any other exceptional condition.
     */
    @Override
    public abstract InputStream newInputStream()
    throws IOException;
}
