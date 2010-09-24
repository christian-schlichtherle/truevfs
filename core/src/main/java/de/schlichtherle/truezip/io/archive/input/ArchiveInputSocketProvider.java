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
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocketProvider;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.InputSocketProvider;
import java.io.IOException;

/**
 * Provides {@link InputSocket}s for read access to archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the archive entries.
 * @see     ArchiveOutputSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveInputSocketProvider<AE extends ArchiveEntry>
extends InputSocketProvider<AE, ArchiveEntry> {

    /**
     * {@inheritDoc}
     * <p>
     * The given archive entry is guaranteed to be one of the entries in this
     * container.
     *
     * @param entry a non-{@code null} archive entry.
     */
    @Override
    ArchiveInputSocket<AE> getInputSocket(AE entry)
    throws IOException;
}
