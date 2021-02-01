/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.ChannelInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

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
public abstract class AbstractInputSocket<E extends Entry> extends AbstractIoSocket<E> implements InputSocket<E> {

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
    public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
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
    public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        throw new UnsupportedOperationException();
    }
}
