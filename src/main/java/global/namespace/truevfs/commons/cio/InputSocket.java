/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import global.namespace.truevfs.commons.io.ChannelInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A <em>stateless</em> factory for input streams and seekable byte channels
 * which operate on a {@linkplain #getTarget() target entry}.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 * @see OutputSocket
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface InputSocket<E extends Entry> extends IoSocket<E> {

    /**
     * Returns a new input stream for reading bytes.
     * The returned input stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * The default implementation calls {@link #channel(Optional)} and wraps the result in a {@link ChannelInputStream}
     * adapter.
     * Note that this violates the contract for this method unless you override either this method or
     * {@link #channel(Optional)}.
     *
     * @param peer the optional peer socket for copying entry contents.
     * @return A new input stream for reading bytes.
     * @throws IOException on any I/O error.
     */
    default InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        return new ChannelInputStream(channel(peer));
    }

    /**
     * <b>Optional operation:</b> Returns a new seekable byte channel for reading bytes.
     * If this operation is supported, then the returned seekable byte channel should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is input, the returned channel may not be able to write data and any
     * attempt to do so should fail with a {@link NonWritableChannelException}.
     * <p>
     * The default implementation always throws an {@link UnsupportedOperationException}.
     *
     * @param peer the optional peer socket for copying entry contents.
     * @return A new seekable byte channel for reading bytes.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IOException                   on any I/O error.
     */
    default SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        throw new UnsupportedOperationException();
    }
}
