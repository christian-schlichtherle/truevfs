/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.ChannelInputStream;
import de.truezip.kernel.io.Source;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract factory for input resources for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. any input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 *
 * @param  <E> the type of the {@link #getLocalTarget() local target}
 *         for I/O operations.
 * @see    OutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class InputSocket<E extends Entry>
extends IOSocket<E, Entry> implements Source {

    @CheckForNull
    private OutputSocket<?> peer;

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
     * @param  to the input socket with the peer target to inherit.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    public final InputSocket<E> bind(final InputSocket<?> to) {
        if (this == to)
            throw new IllegalArgumentException();
        this.peer = to.peer;
        return this;
    }

    /**
     * Connects this input socket to the given peer output socket.
     * Note that this method changes the peer input socket of
     * the given peer output socket to this instance.
     *
     * @param  newPeer the nullable peer output socket to connect to.
     * @return {@code this}
     */
    final InputSocket<E> connect(@CheckForNull final OutputSocket<?> newPeer) {
        final OutputSocket<?> oldPeer = peer;
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
     * Implementations must support calling this method multiple times.
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #newChannel()} and wraps the result in a
     * {@link ChannelInputStream} adapter.
     * Note that this may intentionally violate the contract for this method
     * because {@link #newChannel()} may throw an
     * {@link UnsupportedOperationException} while this method may not,
     * so override appropriately.
     */
    @Override
    public InputStream newStream() throws IOException {
        return new ChannelInputStream(newChannel());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementations must support calling this method multiple times.
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link InputSocket} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel newChannel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
