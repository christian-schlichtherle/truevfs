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
package de.schlichtherle.truezip.io.entry;

import de.schlichtherle.truezip.io.entry.Entry.Type;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * An immutable, thread-safe factory for entries.
 *
 * @param   <E> The type of the created entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@ThreadSafe
public interface EntryFactory<E extends Entry> {

    /**
     * Returns a new entry for the given name.
     * The implementation may need to fix this name in order to 
     * form a valid {@link Entry#getName() entry name} for their
     * particular requirements.
     * <p>
     * If {@code template} is not {@code null}, then the returned entry shall
     * inherit as much properties from this template as possible - with the
     * exception of its name and type.
     * Furthermore, if {@code name} and {@code type} are equal to the name and
     * type of this template, then the returned entry shall be a clone of the
     * template which shares no mutable objects with the template.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     */
    @NonNull E
    newEntry(   @NonNull String name,
                @NonNull Type type,
                @CheckForNull Entry template)
    throws CharConversionException;
}
