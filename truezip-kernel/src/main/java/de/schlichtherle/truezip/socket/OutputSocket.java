/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract factory for output resources for writing bytes to its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between output sockets and input sockets
 * is n:1, i.e. any output socket can have at most one peer input socket, but
 * it may be the peer of many other input sockets.
 *
 * @param   <E> the type of the {@link #getLocalTarget() local target}
 *          for I/O operations.
 * @see     InputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
// TODO: Extract Sink interface for the new*() methods.
@NotThreadSafe
public abstract class OutputSocket<E extends Entry>
extends IOSocket<E, Entry> {

    @CheckForNull
    private InputSocket<?> peer;

    /**
     * {@inheritDoc}
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * {@link #connect}ed to another socket.
     * 
     * @throws IOException On any I/O failure.
     */
    // See https://java.net/jira/browse/TRUEZIP-203
    @Override
    public final @Nullable Entry getPeerTarget() throws IOException {
        return null == peer ? null : peer.getLocalTarget();
    }

    /**
     * Binds this socket to given socket by inheriting its
     * {@link #getPeerTarget() peer target}.
     * Note that this method does <em>not</em> change the state of the
     * given socket and does <em>not</em> connect this socket to the peer
     * socket, that is it does not set this socket as the peer of of the given
     * socket.
     *
     * @param  to the output socket with the peer target to inherit.
     * @return {@code this}
     */
    public final OutputSocket<E> bind(final OutputSocket<?> to) {
        this.peer = to.peer;
        return this;
    }

    /**
     * Connects this output socket to the given peer input socket.
     * Note that this method changes the peer output socket of
     * the given peer input socket to this instance.
     *
     * @param  newPeer the nullable peer input socket to connect to.
     * @return {@code this}
     */
    final OutputSocket<E> connect(@CheckForNull final InputSocket<?> newPeer) {
        final InputSocket<?> oldPeer = peer;
        if (oldPeer != newPeer) {
            if (null != oldPeer) {
                peer = null;
                oldPeer.connect(null);
            }
            if (null != newPeer) {
                peer = newPeer;
                newPeer.connect(this);
            }
        }
        return this;
    }

    /**
     * <b>Optional:</b> Returns a new seekable byte channel for writing bytes
     * to the {@link #getLocalTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned seekable byte channel should <em>not</em> be
     * buffered.
     * Buffering should be addressed by client applications instead.
     * 
     * @return A new seekable byte channel.
     * @throws IOException On any I/O failure.
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @since  TrueZIP 7.2
     */
    @CreatesObligation
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new output stream for writing bytes to the
     * {@link #getLocalTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned output stream should <em>not</em> be buffered.
     * Buffering should be addressed by the caller instead - see
     * {@link IOSocket#copy}.
     *
     * @return A new output stream.
     * @throws IOException On any I/O failure.
     */
    @CreatesObligation
    public abstract OutputStream newOutputStream() throws IOException;
}
