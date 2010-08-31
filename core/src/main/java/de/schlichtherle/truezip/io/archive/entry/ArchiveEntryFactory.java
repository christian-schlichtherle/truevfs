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

import java.io.CharConversionException;

/**
 * An immutable, thread-safe factory for archive entries.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @param <AE> The type of the instances returned by {@link #newEntry}.
 */
public interface ArchiveEntryFactory {

    /**
     * Creates a new archive entry with the given entry {@code name}.
     *
     * @param  name a valid archive entry name  - never <code>null</code>.
     * @param  template if not {@code null}, then the returned new archive
     *         entry shall inherit as much properties from this object as
     *         possible - with the exception of its entry name.
     *         This is typically used for copy operations.
     * @return A new archive entry object - {@code null} is not permitted.
     * @throws CharConversionException If {@code name} contains illegal
     *         characters.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    ArchiveEntry newArchiveEntry(String name, ArchiveEntry template)
    throws CharConversionException;
}
