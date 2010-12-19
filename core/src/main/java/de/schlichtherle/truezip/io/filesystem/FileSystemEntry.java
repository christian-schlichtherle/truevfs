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

import de.schlichtherle.truezip.io.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Set;

/**
 * A file system entry is an entry which can list directory members.
 * Optionally, it may also provide access to another entry which is decorated
 * by it.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileSystemEntry implements Entry {

    /**
     * Returns the non-{@code null} <i>file system entry name</i>.
     * A file system entry name is an {@link Entry#getName() entry name}
     * which must conform to the following additional constraints:
     * <ol>
     * <li>A file system entry name must be in normal form,
     *     i.e. it must not contain redundant {@code "."} and {@code ".."}
     *     segments.
     * <li>A file system entry name must not equal {@code "."}.
     * <li>A file system entry name must not equal {@code ".."}.
     * <li>A file system entry name must not start with {@code "/"}.
     * <li>A file system entry name must not start with {@code "./"}.
     * <li>A file system entry name must not start with {@code "../"}.
     * <li>A file system entry name must not end with {@code "/"}.
     * </ol>
     *
     * @return The non-{@code null} <i>file system entry name</i>.
     * @see    FileSystemEntryName
     */
    @Override
    @NonNull
    public abstract String getName();

    /**
     * If this is not a directory entry, {@code null} is returned.
     * Otherwise, an unmodifiable set of strings is returned which
     * represent the base names of the members of this directory entry.
     */
    @Nullable
    public abstract Set<String> getMembers();

    /**
     * Two file system entries are considered equal if and only if they are
     * identical.
     * This can't get overriden.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[name=")
                .append(getName())
                .append(",members=")
                .append(getMembers())
                .append("]")
                .toString();
    }
}
