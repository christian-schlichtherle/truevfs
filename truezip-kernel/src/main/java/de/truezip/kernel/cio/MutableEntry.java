/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import javax.annotation.CheckForNull;

/**
 * Represents a mutable entry in a container.
 *
 * @author Christian Schlichtherle
 */
public interface MutableEntry extends Entry {

    /**
     * Sets the size of the given {@code type} for this entry.
     *
     * @param  type the type of the size.
     * @param  value the value of the size in bytes
     *         or {@link #UNKNOWN} if not specified.
     * @return {@code false} if and only if setting a size of the given
     *         {@code type} is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     *         {@link #UNKNOWN}.
     */
    boolean setSize(Size type, long value);

    /**
     * Sets the time of the given access {@code type} for this entry.
     *
     * @param  type the type of the access.
     * @param  value the value of the size in in milliseconds since the epoch
     *         or {@link #UNKNOWN} if not specified.
     * @return {@code false} if and only if setting a time of the given
     *         {@code type} is unsupported.
     * @throws NullPointerException if {@code type} is {@code null}.
     * @throws IllegalArgumentException if {@code value} is negative and not
     *         {@link #UNKNOWN}.
     */
    boolean setTime(Access type, long value);

    /**
     * Sets the permission for the given {@code entity} for the given access
     * {@code type} to this entry.
     * 
     * @param  entity the entity which desires access.
     * @param  type the type of the access.
     * @param  value the value of the permission
     *         or {@code null} if not specified.
     * @return {@code false} if and only if setting a permission of the given
     *         {@code type} for the given {@code entity} is unsupported.
     */
    boolean setPermitted(
            Access type,
            Entity entity,
            @CheckForNull Boolean value);
}
