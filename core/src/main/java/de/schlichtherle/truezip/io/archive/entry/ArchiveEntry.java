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

import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryMetaData;

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
 * <h3><a name="entryName">Requirements for Archive Entry Names</a></h3>
 * <p>
 * Implementations must meet the following requirements for archive entry
 * names:
 * <ol>
 * <li>An entry name is a sequence of file or directory entity
 *     <i>base names</i> which are separated by one or more <i>separator
 *     characters</i> ({@link #SEPARATOR_CHAR}).
 *     This implies that a base name cannot contain separator characters.
 * <li>An entry name may contain one or more dot ({@code "."}) or dot-dot
 *     ({@code ".."}) base names which represent the current or parent
 *     directory respectively.
 * <li>An entry name may start with one or more separator characters
 *     - this is called an <i>absolute entry name</i>.
 * <li>An entry name may end with one or more separator characters,
 *     e.g. to identify a directory entry.
 * </ol>
 * For example, {@code "foo/bar"} and {@code "./abc/../foo/./def/./../bar/./"}
 * are both valid entry names which refer to the same entity in the archive
 * file.
 * <p>
 * Note that these requirements have been relaxed in contrast to TrueZIP 6 in
 * order to ease the implementation of archive drivers and separate concerns.
 * The additional restrictions imposed by TrueZIP 6 must now be handled by the
 * client, which is usually an archive file system object.
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

    /**
     * Defines the type of archive entry.
     */
    public enum Type {
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
     * Returns the archive entry name.
     *
     * @return A non-null reference.
     * @see <a href="#entryName">Requirements for Archive Entry Names</a>
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

    /**
     * Returns the meta data for this archive entry.
     * The default value is {@code null}.
     *
     * @deprecated Remove this - it's not a concern of an archive entry!
     */
    ArchiveEntryMetaData getMetaData();

    /**
     * Sets the meta data for this archive entry.
     *
     * @param metaData The meta data - may be {@code null}.
     *
     * @deprecated Remove this - it's not a concern of an archive entry!
     */
    void setMetaData(ArchiveEntryMetaData metaData);
}
