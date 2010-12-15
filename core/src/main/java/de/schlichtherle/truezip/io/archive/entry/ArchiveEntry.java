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

import de.schlichtherle.truezip.io.entry.Entry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * In general, if a property has an unknown value, its getter method must
 * return the value
 * {@value de.schlichtherle.truezip.io.entry.Entry#UNKNOWN}
 * or {@code null} respectively.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntry extends Entry {

    /**
     * Sets the size of this archive entry.
     *
     * @param  type the size type.
     * @param  value the size of the given size type for this archive entry in
     *         bytes or
     *         {@value de.schlichtherle.truezip.io.entry.Entry#UNKNOWN}.
     * @throws IllegalArgumentException if {@code size} is negative and not
     *         {@value de.schlichtherle.truezip.io.entry.Entry#UNKNOWN}.
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
     *         {@value de.schlichtherle.truezip.io.entry.Entry#UNKNOWN}.
     * @throws IllegalArgumentException if {@code time} is negative and not
     *         {@value de.schlichtherle.truezip.io.entry.Entry#UNKNOWN}.
     * @return {@code true} on success, {@code false} otherwise, e.g. if the
     *         given type is unsupported.
     */
    boolean setTime(Access type, long value);
}
