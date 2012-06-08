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
 * @param  <LT> the type of the {@linkplain #localTarget() local target}
 *         for I/O operations.
 * @param  <PT> the type of the {@linkplain #peerTarget() peer target}
 *         for I/O operations.
 * @param  <Local> the representational type of
 *         {@linkplain #connect(IoSocket) this socket}.
 * @param  <Peer> the type of the {@linkplain #getPeer() peer socket}.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class AbstractIoSocket<
        LT extends Entry,
        PT extends Entry,
        Local extends IoSocket<LT, PT, Local, Peer>,
        Peer extends IoSocket<? extends PT, ?, ?, ? super Local>>
implements IoSocket<LT, PT, Local, Peer> {

    private @CheckForNull Peer peer;

    // See https://java.net/jira/browse/TRUEZIP-203
    @Override
    public final PT peerTarget() throws IOException {
        return null != peer ? peer.localTarget() : null;
    }

    @Override public final Peer getPeer() { return peer; }

    @Override
    @SuppressWarnings("unchecked")
    public final Local connect(final @CheckForNull Peer np) {
        final Peer op = peer;
        if (op != np) {
            if (null != op) {
                peer = null;
                op.connect(null);
            }
            if (null != np) {
                peer = np;
                np.connect((Local) this);
            }
        }
        return (Local) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Local bind(final IoSocket<?, ?, ?, ? extends Peer> to) {
        if (this == to) throw new IllegalArgumentException();
        this.peer = to.getPeer();
        return (Local) this;
    }

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
