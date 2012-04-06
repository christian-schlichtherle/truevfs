/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * @author Christian Schlichtherle
 */
public abstract class AbstractSink implements Sink {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractSink} calls
     * {@link #newChannel()} and wraps the result in a
     * {@link SeekableByteChannelOutputStream} adapter.
     * Note that this may intentionally violate the contract for this method
     * because {@link #newChannel()} may throw an
     * {@link UnsupportedOperationException} while this method may not,
     * so override appropriately.
     */
    @Override
    public OutputStream newStream() throws IOException {
        return new SeekableByteChannelOutputStream(newChannel());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link AbstractSink} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel newChannel() throws IOException {
        throw new UnsupportedOperationException();
    }
}