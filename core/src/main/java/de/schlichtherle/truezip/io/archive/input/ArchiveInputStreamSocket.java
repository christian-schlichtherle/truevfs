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
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.io.socket.InputStreamSocket;
import de.schlichtherle.truezip.io.socket.IOReference;
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
 * @param   <AE> The type of the {@link #get() local target} archive entry.
 * @see     ArchiveOutputStreamSocket
 * @see     ArchiveInput
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveInputStreamSocket<AE extends ArchiveEntry>
extends InputStreamSocket<AE, ArchiveEntry> {

    /**
     * Returns the non-{@code null} local target archive entry.
     * <p>
     * Client applications must not change the state of the returned archive
     * entry.
     *
     * @return The non-{@code null} local archive entry target.
     */
    @Override
    AE get();

    /**
     * {@inheritDoc}
     *
     * @param  destination a nullable reference to a peer archive entry which
     *         is going to be written in order to copy the data from the
     *         {@link #get() target} archive entry.
     *         <p>
     *         Implementations may test the runtime type of this object in
     *         order to check if they should set up the returned input stream
     *         appropriately.
     *         <p>
     *         For example, the ZIP driver family uses this to copy the
     *         deflated entry data directly without recompressing it.
     *         As another example, the TAR driver family uses this to determine
     *         the size of the input file, thereby removing the need to create
     *         (yet another) temporary file.
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
    InputStream newInputStream(IOReference<? extends ArchiveEntry> destination)
    throws IOException;
}
