/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.ChannelOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A <em>stateless</em> factory for output streams and seekable byte channels
 * which operate on a {@linkplain #getTarget() target entry}.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O
 *            operations.
 * @author Christian Schlichtherle
 * @see InputSocket
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface OutputSocket<E extends Entry> extends IoSocket<E> {

    /**
     * Returns a new output stream for writing bytes.
     * The returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * The default implementation calls {@link #channel(Optional)} and wraps the result in a {@link ChannelOutputStream}
     * adapter.
     * Note that this violates the contract for this method unless you override either this method or
     * {@link #channel(Optional)}.
     *
     * @param peer the optional peer socket for copying entry contents.
     * @return A new output stream for writing bytes.
     * @throws IOException on any I/O error.
     */
    default OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return new ChannelOutputStream(channel(peer));
    }

    /**
     * <b>Optional operation:</b> Returns a new seekable byte channel for writing bytes.
     * If this operation is supported, then the returned seekable byte channel should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is output, the returned channel may not be able to position the file
     * pointer or read data and any attempt to do may should fail with a {@link NonReadableChannelException}.
     * <p>
     * The default implementation always throws an {@link UnsupportedOperationException}.
     *
     * @param peer the optional peer socket for copying entry contents.
     * @return A new seekable byte channel for writing bytes.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IOException                   on any I/O error.
     */
    default SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        throw new UnsupportedOperationException();
    }
}
