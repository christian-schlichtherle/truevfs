/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import net.truevfs.kernel.io.ChannelInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract factory for input resources for reading bytes from its
 * <i>local target</i>.
 *
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractInputSocket<E extends Entry>
extends AbstractIOSocket<E, Entry>
implements InputSocket<E> {

    private @CheckForNull OutputSocket<?> peer;

    @Override
    public final OutputSocket<? extends Entry> getPeerSocket() {
        return peer;
    }

    @Override
    public final InputSocket<E> bind(final InputSocket<? extends Entry> to) {
        if (this == to)
            throw new IllegalArgumentException();
        this.peer = to.getPeerSocket();
        return this;
    }

    @Override
    public final InputSocket<E> connect(
            final @CheckForNull OutputSocket<? extends Entry> np) {
        final OutputSocket<?> op = peer;
        if (op != np) {
            if (null != op) {
                peer = null;
                op.connect(null);
            }
            if (null != np) {
                peer = np;
                np.connect(this);
            }
        }
        return this;
    }

    // See https://java.net/jira/browse/TRUEZIP-203
    @Override
    public final @CheckForNull Entry peerTarget() throws IOException {
        return null == peer ? null : peer.localTarget();
    }

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
