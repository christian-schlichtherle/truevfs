/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.io.ChannelInputStream;

/**
 * An abstract factory for input resources for reading bytes from its
 * <i>local target</i>.
 *
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @see    AbstractOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractInputSocket<E extends Entry>
extends AbstractIoSocket<E, Entry, InputSocket<E>, OutputSocket<? extends Entry>>
implements InputSocket<E> {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #channel()} and wraps the result in a
     * {@link ChannelInputStream} adapter.
     * Note that this violates the contract for this method unless you
     * override either this method or {@link #channel()} with a valid
     * implementation.
     */
    @Override
    public InputStream stream() throws IOException {
        return new ChannelInputStream(channel());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link InputSocket} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
