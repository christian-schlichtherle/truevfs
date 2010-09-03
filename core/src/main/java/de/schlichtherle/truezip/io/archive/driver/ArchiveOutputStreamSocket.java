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

import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.socket.OutputStreamSocket;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates output streams for writing bytes to its target archive entry.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param   <AE> The type of the {@link #getTarget() target} output archive
 *          entry.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveOutputStreamSocket<AE extends ArchiveEntry>
extends OutputStreamSocket<AE, ArchiveEntry> {

    /**
     * {@inheritDoc}
     *
     * @param  source if not {@code null}, this references the target
     *         input archive entry in an {@link InputArchive input archive}
     *         which is going to be read in order to copy its data to the
     *         {@link #getTarget() target} output archive entry of this
     *         instance.
     *         <p>
     *         Implementations may test the runtime type of this object in
     *         order to check if they should copy some class-specific
     *         properties from the input archive entry to the output archive
     *         entry or set up the returned output stream appropriately.
     *         <p>
     *         For example, the ZIP driver family uses this to copy the
     *         deflated entry data directly without recompressing it.
     *         As another example, the TAR driver family uses this to determine
     *         the size of the input file, thereby removing the need to create
     *         (yet another) temporary file.
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
    OutputStream newOutputStream(IOReference<? extends ArchiveEntry> source)
    throws IOException;
}
