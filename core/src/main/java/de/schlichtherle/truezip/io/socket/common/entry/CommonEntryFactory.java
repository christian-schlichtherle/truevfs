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

package de.schlichtherle.truezip.io.socket.common.entry;

import de.schlichtherle.truezip.io.socket.common.entry.CommonEntry.Type;
import java.io.CharConversionException;

/**
 * An immutable, thread-safe factory for common entries.
 *
 * @param <CE> The type of the common entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface CommonEntryFactory<CE extends CommonEntry> {

    /**
     * Returns a new common entry for the given name.
     * The implementation may need to fix this name in order to 
     * form a valid {@link CommonEntry#getName() entry name} for their
     * particular requirements.
     *
     * @param  name a non-{@code null} entry name.
     * @param  type a non-{@code null} entry type.
     * @param  template if not {@code null}, then the new archive file system
     *         entry shall inherit as much properties from this common entry
     *         as possible - with the exception of its name and type.
     * @return A new common entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     * @throws NullPointerException if {@code name} or {@code type} are
     *         {@code null}.
     */
    CE newEntry(String name, Type type, CommonEntry template)
    throws CharConversionException;
}
