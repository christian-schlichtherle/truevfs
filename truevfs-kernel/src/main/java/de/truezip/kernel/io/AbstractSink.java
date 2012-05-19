/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * An abstract provider for output streams or seekable byte channels.
 * 
 * @author Christian Schlichtherle
 */
public abstract class AbstractSink implements Sink {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractSink} calls
     * {@link #channel()} and wraps the result in a
     * {@link ChannelOutputStream} adapter.
     * Note that this violates the contract for this method unless you
     * override either this method or {@link #channel()} with a valid
     * implementation.
     */
    @Override
    public OutputStream stream() throws IOException {
        return new ChannelOutputStream(channel());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link AbstractSink} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
