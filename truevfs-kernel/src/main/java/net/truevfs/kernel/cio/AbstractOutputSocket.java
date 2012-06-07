/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.io.ChannelOutputStream;

/**
 * An abstract factory for output resources for writing bytes to its
 * <i>local target</i>.
 *
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @see    AbstractInputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractOutputSocket<E extends Entry>
extends AbstractIoSocket<E, Entry, OutputSocket<E>, InputSocket<? extends Entry>>
implements OutputSocket<E> {

    private @CheckForNull InputSocket<? extends Entry> peer;

    @Override
    public final InputSocket<? extends Entry> getPeer() {
        return peer;
    }

    @Override
    public final OutputSocket<E> bind(final IoSocket<?, ?, ?, ? extends InputSocket<? extends Entry>> to) {
        if (this == to) throw new IllegalArgumentException();
        this.peer = to.getPeer();
        return this;
    }

    @Override
    public final OutputSocket<E> connect(
            final @CheckForNull InputSocket<? extends Entry> np) {
        final InputSocket<?> op = peer;
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
     * The implementation in the class {@link OutputSocket} calls
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
     *         {@link OutputSocket} <em>always</em> throws an exception of
     *         this type.
     */
    @Override
    public SeekableByteChannel channel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
