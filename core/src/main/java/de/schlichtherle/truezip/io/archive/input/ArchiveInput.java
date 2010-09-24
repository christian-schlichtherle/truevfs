/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.socket.common.input.CommonInputSocketProvider;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
import java.io.Closeable;

/**
 * An archive entry container which supports reading archive entries from an
 * arbitrary input source.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the archive entries.
 * @see     ArchiveOutput
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveInput<AE extends ArchiveEntry>
extends ArchiveEntryContainer<AE>,
        CommonInputSocketProvider<AE>,
        Closeable {
}
