/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.ChannelInputStream;

/**
 * Abstract base class for input sockets.
 * <p>
 * Subclasses should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    AbstractOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractInputSocket<E extends Entry>
extends AbstractIoSocket<E> implements InputSocket<E> {

    /**
     * Returns the target of the given nullable peer socket or null.
     * This method is provided for convenience.
     * 
     * @param  <E> the type of the peer socket's local target.
     * @param  peer the nullable peer socket.
     * @return {@code null} if the given reference is null or the target of the
     *         given peer socket otherwise.
     * @throws IOException if resolving the peer's local target fails.
     */
    protected final @CheckForNull <E extends Entry> E target(
            @CheckForNull OutputSocket<E> peer)
    throws IOException {
        return null != peer ? peer.target() : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #channel} and wraps the result in a
     * {@link ChannelInputStream} adapter.
     * Note that this violates the contract for this method unless you
     * override either this method or {@link #channel} with a valid
     * implementation.
     */
    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new ChannelInputStream(channel(peer));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link InputSocket} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        throw new UnsupportedOperationException();
    }
}
