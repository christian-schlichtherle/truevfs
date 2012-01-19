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
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * An abstract factory for input resources for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. any input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 *
 * @param   <E> the type of the {@link #getLocalTarget() local target}
 *          for I/O operations.
 * @see     OutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class InputSocket<E extends Entry>
extends IOSocket<E, Entry> {

    @CheckForNull
    private OutputSocket<?> peer;

    /**
     * {@inheritDoc}
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * {@link #connect}ed to another socket.
     */
    @Override
    public @Nullable Entry getPeerTarget() throws IOException {
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
     */
    public final InputSocket<E> bind(@CheckForNull final InputSocket<?> to) {
        this.peer = null != to ? to.peer : null;
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
            peer = null;
            if (null != oldPeer)
                oldPeer.connect(null);
            peer = newPeer;
            if (null != newPeer)
                newPeer.connect(this);
        }
        return this;
    }

    /**
     * <b>Optional:</b> Returns a new read only file for reading bytes from
     * the {@link #getLocalTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned read only file should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @throws IOException on any I/O error.
     * @return A new read only file.
     */
    public abstract ReadOnlyFile newReadOnlyFile() throws IOException;

    /**
     * <b>Optional:</b> Returns a new seekable byte channel for reading bytes
     * from the {@link #getLocalTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned seekable byte channel should <em>not</em> be
     * buffered.
     * Buffering should be addressed by client applications instead.
     * 
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @throws IOException on any I/O error.
     * @since  TrueZIP 7.2
     */
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new input stream for reading bytes from the
     * {@link #getLocalTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned input stream should <em>not</em> be buffered.
     * Buffering should be addressed by the caller instead - see
     * {@link IOSocket#copy}.
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #newReadOnlyFile()} and wraps the resulting object in a new
     * {@link ReadOnlyFileInputStream} as an adapter.
     * Note that this may <em>violate</em> the contract for this method because
     * {@link #newReadOnlyFile()} is allowed to throw an
     * {@link UnsupportedOperationException} while this method is not!
     *
     * @throws IOException on any I/O error.
     * @return A new input stream.
     */
    public InputStream newInputStream() throws IOException {
        return new ReadOnlyFileInputStream(newReadOnlyFile());
    }
}
