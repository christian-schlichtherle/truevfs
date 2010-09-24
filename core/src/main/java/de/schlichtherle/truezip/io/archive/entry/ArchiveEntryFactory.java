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

package de.schlichtherle.truezip.io.archive.entry;

import de.schlichtherle.truezip.io.socket.common.CommonEntry;
import de.schlichtherle.truezip.io.socket.common.CommonEntry.Type;
import java.io.CharConversionException;

/**
 * An immutable, thread-safe factory for archive entries.
 *
 * @param <AE> The type of the created archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntryFactory<AE extends ArchiveEntry> {

    /**
     * Returns a new archive entry for the given
     * {@link CommonEntry#getName() common entry name}.
     * The implementation may need to fix this name in order to 
     * form a valid {@link ArchiveEntry#getName() archive entry name} for
     * their particular archive type.
     *
     * @param  name a non-{@code null} <i>common entry name</i>.
     * @param  type a non-{@code null} entry type.
     * @param  template if not {@code null}, then the new archive entry shall
     *         inherit as much properties from this common entry as possible
     *         - with the exception of the archive entry name.
     *         Furthermore, its {@link ArchiveEntry#getType()} method must
     *         return the {@code type} parameter.
     *         This parameter is typically used for copy operations.
     * @return A new archive entry.
     * @throws CharConversionException if {@code name} contains characters
     *         which are not valid in the archive file.
     */
    AE newEntry(String name, Type type, CommonEntry template)
    throws CharConversionException;
}
