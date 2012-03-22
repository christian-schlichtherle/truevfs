/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe.
 * 
 * @author  Christian Schlichtherle
 */
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