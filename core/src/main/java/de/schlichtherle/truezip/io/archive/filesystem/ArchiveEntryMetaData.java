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

package de.schlichtherle.truezip.io.archive.filesystem;

//import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.DIRECTORY;

/**
 * Annotates an {@link ArchiveEntry} with the fields and methods required to
 * implement the concept of a directory.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use!
 * It's only public for technical reasons and may get renamed or entirely
 * disappear without notice.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveEntryMetaData {

    /**
     * This thread local variable returns an {@link ArrayList} which is used
     * as a temporary buffer to implement filtered list methods.
     */
    private static final ThreadLocal<List<?>> threadLocal
            = new ThreadLocal<List<?>>() {
        @Override
        protected List<?> initialValue() {
            return new ArrayList<Object>(64);
        }
    };

    /**
     * If the entry from which this object has been created represents a
     * directory, then this is a valid reference to a set of Strings,
     * representing the children names.
     * Otherwise this field is initialized with {@code null}.
     */
    final Set<String> children;

    /**
     * A package private constructor.
     * Used by the factory in this package only.
     */
    ArchiveEntryMetaData(final ArchiveEntry entry) {
        this.children = entry.getType() == DIRECTORY
                ? new LinkedHashSet<String>()
                : null;
    }

    int size() {
        return children.size();
    }

    /**
     * Visits the children of this directory in arbitrary order.
     *
     * @throws NullPointerException If {@code visitor} is {@code null}.
     */
    void list(final ChildVisitor visitor) {
        visitor.init(children.size());
        for (final String child : children)
            visitor.visit(child);
    }
}
