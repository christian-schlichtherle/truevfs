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

import de.schlichtherle.truezip.io.socket.CommonEntry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * In general, if a property has an unknown value, its getter method must
 * return the value
 * {@value de.schlichtherle.truezip.io.socket.entry.CommonEntry#UNKNOWN}
 * or {@code null} respectively.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntry extends CommonEntry {

    /**
     * Returns the non-{@code null} <i>archive entry name</i>.
     * An archive entry name is a
     * {@link CommonEntry#getName() common entry name} which meets the
     * following additional requirement:
     * <ol>
     * <li>A common entry name may end with one or more separator
     *     characters (e.g. to identify a directory entry).</li>
     * </ol>
     * For example, {@code "foo/bar/"} and
     * {@code "./abc/../foo/./def/./../bar/."} are both valid path names
     * which refer to the same entity in the archive file.
     *
     * @return The non-{@code null} <i>archive entry name</i>.
     */
    @Override
    String getName();

    /**
     * Sets the size of this archive entry.
     *
     * @param  type the size type.
     * @param  value the size of the given size type for this archive entry in
     *         bytes or
     *         {@value de.schlichtherle.truezip.io.socket.entry.CommonEntry#UNKNOWN}.
     * @throws IllegalArgumentException if {@code size} is negative and not
     *         {@value de.schlichtherle.truezip.io.socket.entry.CommonEntry#UNKNOWN}.
     * @return {@code true} on success, {@code false} otherwise, e.g. if the
     *         type is unsupported.
     */
    boolean setSize(Size type, long value);

    /**
     * Sets the last access time of this archive entry.
     *
     * @param  type the access type.
     * @param  value the last time of the given access type for this archive
     *         entry in milliseconds since the epoch or
     *         {@value de.schlichtherle.truezip.io.socket.entry.CommonEntry#UNKNOWN}.
     * @throws IllegalArgumentException if {@code time} is negative and not
     *         {@value de.schlichtherle.truezip.io.socket.entry.CommonEntry#UNKNOWN}.
     * @return {@code true} on success, {@code false} otherwise, e.g. if the
     *         given type is unsupported.
     */
    boolean setTime(Access type, long value);
}
