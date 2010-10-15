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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.util.Link;
import java.util.Set;

/**
 * A file system entry is a common entry which can list directory members.
 * Optionally, it may also provide access to another common entry which is
 * decorated by it.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemEntry<CE extends CommonEntry>
extends CommonEntry, Link<CE> {

    /**
     * Returns the non-{@code null} <i>path name</i>.
     * A path name is a {@link CommonEntry#getName() common entry name}
     * which meets the following additional requirement:
     * <ol>
     * <li>A path name <em>must not</em> end with a separator character.</li>
     * </ol>
     *
     * @return The non-{@code null} <i>path name</i>.
     */
    @Override
    String getName();

    /**
     * If this is not a directory entry, {@code null} is returned.
     * Otherwise, an unmodifiable set of strings is returned which
     * represent the base names of the members of this directory entry.
     */
    Set<String> getMembers();

    /**
     * {@inheritDoc}
     *
     * @return The decorated common entry or {@code this} if this file system
     *         entry does not decorate a common entry or does not want to
     *         provide access to it.
     *         {@code null} is an illegal return value.
     */
    @Override
    CE getTarget();
}
