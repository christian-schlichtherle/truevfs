/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Abstract base class for I/O sockets.
 * 
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractIoSocket<T extends Entry>
implements IoSocket<T> {

    /**
     * Two I/O socket are considered equal if and only if
     * they are identical.
     * 
     * @param  that the object to compare.
     * @return {@code this == that}. 
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(@CheckForNull Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * 
     * @return A hash code which is consistent with {@link #equals}.
     * @see Object#hashCode
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        Object lt;
        try {
            lt = target();
        } catch (final IOException ex) {
            lt = ex;
        }
        return String.format("%s[target=%s]", getClass().getName(), lt);
    }
}
