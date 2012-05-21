/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Abstract base class for I/O sockets.
 * 
 * @param  <LT> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @param  <PT> the type of the {@link #peerTarget() peer target}
 *         for I/O operations.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class AbstractIoSocket<LT extends Entry, PT extends Entry>
implements IoSocket<LT, PT> {

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
     * Returns a string representing a connection of the local and peer
     * targets.
     */
    @Override
    public String toString() {
        Object lt;
        try {
            lt = localTarget();
        } catch (final IOException ex) {
            lt = ex;
        }
        Object pt;
        try {
            pt = peerTarget();
        } catch (final IOException ex) {
            pt = ex;
        }
        return String.format("%s[localTarget=%s, peerTarget=%s]",
                getClass().getName(), lt, pt);
    }
}
