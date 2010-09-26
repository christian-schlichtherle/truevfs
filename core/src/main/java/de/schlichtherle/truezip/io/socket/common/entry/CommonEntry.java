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
package de.schlichtherle.truezip.io.socket.common.entry;

/**
 * Represents an entry in an entry container, e.g. an archive file or a file
 * system.
 * <p>
 * In general, if a property has an unknown value, its getter method must
 * return the value {@value #UNKNOWN} or {@code null} respectively.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface CommonEntry {

    /**
     * The entry name of the root directory, which is {@value}.
     * Note that this name is empty and hence does not contain a
     * separator character.
     *
     * @see #SEPARATOR_CHAR
     */
    String ROOT = "";

    /**
     * The separator character for base names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR
     */
    char SEPARATOR_CHAR = '/';

    /**
     * The separator string for base names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR_CHAR
     */
    String SEPARATOR = "/";

    /**
     * The unknown value for numeric properties,
     * which is {@value}.
     */
    byte UNKNOWN = -1;

    /** Defines the type of archive entry. */
    enum Type {

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
         * resource, e.g. a hard disk or a network service.
         */
        SPECIAL
    }

    enum Access {
        WRITE,
        READ,
    }

    /**
     * Returns the non-{@code null} <i>common entry name</i>.
     * A common entry name must meet the following requirements:
     * <ol>
     * <li>A common entry name is a sequence of <i>segments</i> which are
     *     separated by one or more <i>separator characters</i>
     *     ({@link #SEPARATOR_CHAR}).
     *     This implies that a segment cannot contain separator characters.</li>
     * <li>A common entry name may contain one or more dot ({@code "."}) or
     *     dot-dot ({@code ".."}) segments which represent the current or
     *     parent directory respectively.</li>
     * <li>If a common entry name starts with one or more separator characters
     *     its said to be <i>absolute</i>.
     *     Otherwise, its said to be <i>relative</i>.</li>
     * </ol>
     * For example, {@code "foo/bar"} and
     * {@code "./abc/../foo/./def/./../bar/."} are both valid common entry
     * names which refer to the same entity.
     * <p>
     * Note that implementations may impose additional terms for a common
     * entry name to meet their particular requirements.
     *
     * @return The non-{@code null} <i>common entry name</i>.
     */
    String getName();

    /**
     * Return the archive entry type.
     *
     * @return A non-null reference.
     * @see Type
     */
    Type getType();

    /**
     * Returns the (uncompressed) size of the archive entry in bytes,
     * or {@link #UNKNOWN} if not specified.
     * This method is not meaningful for directory entries.
     */
    long getSize();

    /**
     * Returns the last time of the given access type for this archive entry
     * in milliseconds since the epoch or {@value #UNKNOWN}.
     */
    long getTime(Access type);
}
