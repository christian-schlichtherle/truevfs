/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.entry.FilterArchiveEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.output.FilterArchiveOutput;
import java.io.IOException;

/**
 * A decorator for archive inputs.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The type of the archive entries.
 * @see FilterArchiveOutput
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterArchiveInput<AE extends ArchiveEntry>
extends FilterArchiveEntryContainer<AE>
implements ArchiveInput<AE> {

    protected ArchiveInput<AE> target;

    public FilterArchiveInput(final ArchiveInput<AE> target) {
        this.target = target;
    }

    @Override
    protected ArchiveInput<AE> getTarget() {
        return target;
    }

    @Override
    public ArchiveInputStreamSocket<? extends AE> getInputStreamSocket(
            final AE entry)
    throws IOException {
        return getTarget().getInputStreamSocket(entry);
    }

    @Override
    public void close() throws IOException {
        getTarget().close();
    }
}
