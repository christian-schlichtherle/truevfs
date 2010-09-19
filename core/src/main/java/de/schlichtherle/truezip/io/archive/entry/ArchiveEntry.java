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

package de.schlichtherle.truezip.io.archive.entry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
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
public interface ArchiveEntry {

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
    
    /**
     * Returns the non-{@code null} <i>entry name</i>.
     * An entry name is a <i>path name</i> which meets all additional
     * requirements which may be defined by their particular archive type.
     *
     * @see    ArchiveEntryFactory#newArchiveEntry Common Requirements For Path Names
     * @return The non-{@code null} <i>entry name</i>.
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
     * Sets the (uncompressed) size of this archive entry in bytes.
     *
     * @param  size the (uncompressed) size of this archive entry in bytes
     *         or {@value #UNKNOWN}.
     * @throws IllegalArgumentException if {@code size} is negative and not
     *         {@value #UNKNOWN}.
     */
    void setSize(long size);

    /**
     * Returns the last modification time of this archive entry since the
     * epoch or {@value #UNKNOWN}.
     *
     * @see #setTime
     */
    long getTime();

    /**
     * Sets the last modification time of this archive entry.
     *
     * @param  time the last modification time of this archive entry in
     *         milliseconds since the epoch or {@value #UNKNOWN}.
     * @throws IllegalArgumentException if {@code time} is negative and not
     *         {@value #UNKNOWN}.
     */
    void setTime(long time);
}
