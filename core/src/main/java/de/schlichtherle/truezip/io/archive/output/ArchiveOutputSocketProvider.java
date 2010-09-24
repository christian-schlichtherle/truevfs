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
import de.schlichtherle.truezip.io.archive.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocketProvider;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocketProvider;
import java.io.IOException;

/**
 * Provides {@link OutputSocket}s for write access to archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the archive entries.
 * @see     ArchiveInputSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveOutputSocketProvider<AE extends ArchiveEntry>
extends OutputSocketProvider<AE, CommonEntry> {

    /**
     * {@inheritDoc}
     * <p>
     * It is an error to write an archive entry header or adding the archive
     * entry merely upon the call to this method.
     *
     * @param entry a non-{@code null} archive entry.
     */
    @Override
    ArchiveOutputSocket<AE> getOutputSocket(AE entry) throws IOException;
}
