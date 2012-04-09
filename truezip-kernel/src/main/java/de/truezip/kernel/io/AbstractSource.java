/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * @author Christian Schlichtherle
 */
public abstract class AbstractSource implements Source {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractSource} calls
     * {@link #channel()} and wraps the result in a
     * {@link ChannelInputStream} adapter.
     * Note that this may intentionally violate the contract for this method
     * because {@link #channel()} may throw an
     * {@link UnsupportedOperationException} while this method may not,
     * so override appropriately.
     */
    @Override
    public InputStream stream() throws IOException {
        return new ChannelInputStream(channel());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link AbstractSource} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
