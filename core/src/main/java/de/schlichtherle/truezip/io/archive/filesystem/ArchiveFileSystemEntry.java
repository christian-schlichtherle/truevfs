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
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;

/**
 * Adapts an {@link ArchiveEntry} to a {@link FileSystemEntry}.
 * With the help of this interface, an archive file system can ensure that
 * when a new archive entry is created, the {@code template} parameter is
 * <em>not</em> an instance of this interface, but possibly a product of the
 * archive entry factory in the archive driver.
 * This enables an archive driver to copy properties specific to its type of
 * archive entries, e.g. the compressed size of ZIP entries.
 * 
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystemEntry<E extends ArchiveEntry>
extends FileSystemEntry {

    /**
     * Returns the non-{@code null} archive entry which is adapted by this
     * archive file system entry.
     *
     * @return The non-{@code null} archive entry which is adapted by this
     *         archive file system entry.
     */
    E getArchiveEntry();
}
