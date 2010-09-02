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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.socket.IORef;
import de.schlichtherle.truezip.io.socket.InputStreamSocket;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams for reading bytes from its target archive entry.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param   <AE> The type of the {@link #getTarget() target} input archive
 *          entry.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveInputStreamSocket<AE extends ArchiveEntry>
extends InputStreamSocket<AE, ArchiveEntry> {

    /**
     * {@inheritDoc}
     *
     * @param  destination if not {@code null}, this references the target
     *         output archive entry in an {@link OutputArchive output archive}
     *         which is going to be written in order to copy the data from the
     *         {@link #getTarget() target} input archive entry of this
     *         instance.
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
    InputStream newInputStream(IORef<? extends ArchiveEntry> destination)
    throws IOException;
}
