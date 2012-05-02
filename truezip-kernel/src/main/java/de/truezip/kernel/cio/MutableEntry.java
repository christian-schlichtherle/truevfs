/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

/**
 * Represents a mutable entry in a container.
 *
 * @author Christian Schlichtherle
 */
public interface MutableEntry extends Entry {

    /**
     * Sets the size of the given type for this archive entry.
     *
     * @param  type the type of the size.
     * @param  value the value of the size in bytes or
     * {@value de.truezip.kernel.cio.Entry#UNKNOWN}.
     * @return {@code false} if and only if setting a size for the given type
     * is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     * {@value de.truezip.kernel.cio.Entry#UNKNOWN}.
     */
    boolean setSize(Size type, long value);

    /**
     * Sets the last access time of the given type for this archive entry.
     *
     * @param  type the type of the access time.
     * @param  value the value of the access time in milliseconds since the
     * epoch or {@value de.truezip.kernel.cio.Entry#UNKNOWN}.
     * @return {@code false} if and only if setting a time for the given type
     * is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     * {@value de.truezip.kernel.cio.Entry#UNKNOWN}.
     */
    boolean setTime(Access type, long value);
}
