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

import de.schlichtherle.truezip.io.socket.InputStreamSocketProvider;
import java.io.FileNotFoundException;

public interface ArchiveInputStreamSocketProvider<AE extends ArchiveEntry>
extends InputStreamSocketProvider<AE, ArchiveEntry> {

    /**
     * {@inheritDoc}
     * <p>
     * The diven archive entry is guaranteed to be one of the entries in this
     * container.
     *
     * @param entry a non-{@code null} reference to an archive entry.
     */
    @Override
    ArchiveInputStreamSocket<AE> getInputStreamSocket(AE entry)
    throws FileNotFoundException;
}
