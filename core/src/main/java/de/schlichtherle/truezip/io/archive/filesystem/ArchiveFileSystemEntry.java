/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.socket.common.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.common.entry.FileSystemEntry;

/**
 * A file system entry which refers to an archive entry.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystemEntry
extends FileSystemEntry, IOReference<ArchiveEntry> {

    /**
     * Returns the non-{@code null} <i>path name</i>.
     * A path name is a {@link CommonEntry#getName() common entry name} which
     * meets the following additional requirement:
     * <ol>
     * <li>A path name <em>must not</em> end with a separator character.</li>
     * </ol>
     *
     * @return The non-{@code null} <i>path name</i>.
     */
    @Override
    String getName();
}
