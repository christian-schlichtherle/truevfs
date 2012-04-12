/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.ChannelOutputStream;
import de.truezip.kernel.io.Sink;
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
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @see    InputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class OutputSocket<E extends Entry>
extends IOSocket<E, Entry> implements Sink {

    @CheckForNull
    private InputSocket<?> peer;

    /**
     * {@inheritDoc}
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * {@link #connect}ed to another socket.
     * 
     * @throws IOException On any I/O error.
     */
    // See https://java.net/jira/browse/TRUEZIP-203
    @Override
    public final @Nullable Entry peerTarget() throws IOException {
        return null == peer ? null : peer.localTarget();
    }

    /**
     * Binds this socket to given socket by inheriting its
     * {@link #peerTarget() peer target}.
     * Note that this method does <em>not</em> change the state of the
     * given socket and does <em>not</em> connect this socket to the peer
     * socket, that is it does not set this socket as the peer of of the given
     * socket.
     *
     * @param  to the output socket with the peer target to inherit.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    public final OutputSocket<E> bind(final OutputSocket<?> to) {
        if (this == to)
            throw new IllegalArgumentException();
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
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link OutputSocket} calls
     * {@link #channel()} and wraps the result in a
     * {@link ChannelOutputStream} adapter.
     * Note that this violates the contract for this method unless you
     * override either this method or {@link #channel()} with a valid
     * implementation.
     * 
     * @return A <em>new</em> output stream for writing bytes.
     */
    @Override
    public OutputStream stream() throws IOException {
        return new ChannelOutputStream(channel());
    }

    /**
     * {@inheritDoc}
     * 
     * @return A <em>new</em> seekable byte channel for writing bytes.
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link OutputSocket} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
