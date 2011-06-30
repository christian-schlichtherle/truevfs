/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface FsArchiveEntry extends Entry {

    /**
     * Returns the type of this archive entry.
     *
     * @return The type of this archive entry.
     */
    Type getType();

    /**
     * Sets the size of the given type for this archive entry.
     *
     * @param  type the size type.
     * @param  value the size of the given size type for this archive entry in
     *         bytes or
     *         {@value de.schlichtherle.truezip.entry.Entry#UNKNOWN}.
     * @return {@code false} if and only if setting a size for the given type
     *         is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     *         {@value de.schlichtherle.truezip.entry.Entry#UNKNOWN}.
     */
    boolean setSize(Size type, long value);

    /**
     * Sets the last access time of the given type for this archive entry.
     *
     * @param  type the access type.
     * @param  value the last time of the given access type for this archive
     *         entry in milliseconds since the epoch or
     *         {@value de.schlichtherle.truezip.entry.Entry#UNKNOWN}.
     * @return {@code false} if and only if setting a time for the given type
     *         is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     *         {@value de.schlichtherle.truezip.entry.Entry#UNKNOWN}.
     */
    boolean setTime(Access type, long value);
}
