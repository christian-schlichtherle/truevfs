/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import net.java.truecommons.io.ChannelOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * Abstract base class for output sockets.
 * <p>
 * Subclasses should be immutable.
 *
 * @param <E> the type of the {@linkplain #target() target entry} for I/O
 *            operations.
 * @author Christian Schlichtherle
 * @see AbstractInputSocket
 */
public abstract class AbstractOutputSocket<E extends Entry> extends AbstractIoSocket<E> implements OutputSocket<E> {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link OutputSocket} calls
     * {@link #channel} and wraps the result in a
     * {@link ChannelOutputStream} adapter.
     * Note that this violates the contract for this method unless you
     * override either this method or {@link #channel} with a valid
     * implementation.
     */
    @Override
    public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return new ChannelOutputStream(channel(peer));
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException the implementation in the class
     *                                       {@link OutputSocket} <em>always</em> throws an exception of
     *                                       this type.
     */
    @Override
    public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        throw new UnsupportedOperationException();
    }
}
