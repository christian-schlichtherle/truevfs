/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.Source;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFileInputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
 * is n:1, i.e. any input socket can have at most one remote output socket, but
 * it may be the remote of many other output sockets.
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
    private OutputSocket<?> remote;

    /**
     * {@inheritDoc}
     * <p>
     * The remote target is {@code null} if and only if this socket is not
     * {@link #connect}ed to another socket.
     * 
     * @throws IOException On any I/O failure.
     */
    // See https://java.net/jira/browse/TRUEZIP-203
    @Override
    public final @Nullable Entry getRemoteTarget() throws IOException {
        return null == remote ? null : remote.getLocalTarget();
    }

    /**
     * Binds this socket to given socket by inheriting its
     * {@link #getRemoteTarget() remote target}.
     * Note that this method does <em>not</em> change the state of the
     * given socket and does <em>not</em> connect this socket to the remote
     * socket, that is it does not set this socket as the remote of of the given
     * socket.
     *
     * @param  to the input socket with the remote target to inherit.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    public final InputSocket<E> bind(final InputSocket<?> to) {
        if (this == to)
            throw new IllegalArgumentException();
        this.remote = to.remote;
        return this;
    }

    /**
     * Connects this input socket to the given remote output socket.
     * Note that this method changes the remote input socket of
     * the given remote output socket to this instance.
     *
     * @param  newRemote the nullable remote output socket to connect to.
     * @return {@code this}
     */
    final InputSocket<E> connect(@CheckForNull final OutputSocket<?> newRemote) {
        final OutputSocket<?> oldRemote = remote;
        if (oldRemote != newRemote) {
            if (null != oldRemote) {
                remote = null;
                oldRemote.connect(null);
            }
            if (null != newRemote) {
                remote = newRemote;
                newRemote.connect(this);
            }
        }
        return this;
    }

    /**
     * <b>Optional operation:</b> Returns a new read only file for reading
     * bytes from the {@link #getLocalTarget() local target} in random order.
     * <p>
     * If this method is supported, the implementation must enable calling it
     * any number of times.
     * Furthermore, the returned read only file should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A new read only file.
     * @throws IOException on any I/O failure.
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     */
    @CreatesObligation
    public abstract ReadOnlyFile newReadOnlyFile() throws IOException;

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the abstract class {@link InputSocket} throws an
     * {@link UnsupportedOperationException}.
     */
    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #newReadOnlyFile()} and wraps the result in an
     * {@link ReadOnlyFileInputStream} adapter.
     * Note that this may <em>violate</em> the contract for this method because
     * {@link #newReadOnlyFile()} is allowed to throw an
     * {@link UnsupportedOperationException} while this method is not!
     */
    @Override
    public InputStream newInputStream() throws IOException {
        return new ReadOnlyFileInputStream(newReadOnlyFile());
    }
}
