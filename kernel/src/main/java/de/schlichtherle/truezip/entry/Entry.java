/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.entry;

import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * Represents an entry in an entry container, e.g. an archive file or a file
 * system etc.
 * <p>
 * In general, if a property has an unknown value, its getter method must
 * return the value {@link #UNKNOWN} or {@code null} respectively.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public interface Entry {

    /**
     * The {@code NULL} entry is a dummy entry which may be useful in
     * situations where a non-{@code null} entry is expected but none is
     * available.
     * <p>
     * The {@code NULL} entry has {@code "/dev/random"} as its name,
     * {@link Type#NULL} as its type and {@link #UNKNOWN} for any other
     * property.
     */
    Entry NULL = new Entry() {
        @Override
        public String getName() {
            return "/dev/null";
        }

        @Override
        public Type getType() {
            return Type.NULL;
        }

        @Override
        public long getSize(Size type) {
            return UNKNOWN;
        }

        @Override
        public long getTime(Access type) {
            return UNKNOWN;
        }
    };

    /**
     * The unknown value for numeric properties,
     * which is {@value}.
     */
    byte UNKNOWN = -1;

    /** Defines the type of archive entry. */
    enum Type {

        /**
         * Nonexistent entry.
         * This entry does not exist in the file system.
         */
        NULL,

        /**
         * Regular file.
         * A file usually has some content associated to it which can be read
         * and written using a stream.
         */
        FILE,

        /**
         * Regular directory.
         * A directory can have other archive entries as children.
         */
        DIRECTORY,

        /**
         * Symbolic (named) link.
         * A symbolic link refers to another file system node which could even
         * be located outside the current archive file.
         */
        SYMLINK,

        /**
         * Special file.
         * A special file is a byte or block oriented interface to an arbitrary
         * I/O device, e.g. a hard disk or a network service.
         */
        SPECIAL
    }

    /** Defines the types of size information for an entry. */
    enum Size {
        DATA,
        STORAGE
    }

    /** Defines the types of access information for an entry. */
    enum Access {
        WRITE,
        READ, // TODO: This is not yet fully supported!
    }

    /**
     * Returns the <i>entry name</i>.
     * When parsed, an entry name is interpreted as follows:
     * <ol>
     * <li>An entry name is a sequence of <i>segments</i> which are
     *     separated by one or more <i>separator characters</i>
     *     ({@link EntryName#SEPARATOR_CHAR}).
     *     This implies that a segment cannot contain separator characters.
     * <li>An entry name may contain one or more dot ({@code "."}) or
     *     dot-dot ({@code ".."}) segments which represent the current or
     *     parent segment respectively.
     * <li>An entry name may start with one or more separator characters.
     *     In this case, its said to be <i>absolute</i>.
     *     Otherwise, its said to be <i>relative</i>.
     * <li>An entry name may end with one or more separator
     *     characters (e.g. to identify a directory entry).
     * </ol>
     * For example, the entry names {@code "foo/bar/"} and
     * {@code "./abc/../foo/./def/./../bar/."} both refer to the same entry
     * when being parsed.
     *
     * @return The entry name.
     * @see    EntryName#create(java.lang.String, java.lang.String)
     */
    @NonNull String getName();

    /**
     * Returns the type of this entry.
     *
     * @return The type of this entry.
     */
    @NonNull Type getType();

    /**
     * Returns the size of this entry.
     *
     * @return The size of the given size type for this entry in bytes,
     * or {@link #UNKNOWN} if not specified or the type is unsupported.
     * This method may not be meaningful for non-file entries.
     */
    long getSize(@NonNull Size type);

    /**
     * Returns the last access time of this entry.
     *
     * @return The last time of the given access type for this entry in
     * milliseconds since the epoch or {@value #UNKNOWN} if not specified or
     * the type is unsupported.
     */
    long getTime(@NonNull Access type);
}
