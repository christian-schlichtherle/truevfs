/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A <em>stateless</em> factory for output streams and seekable byte channels
 * which operate on a {@linkplain #target() target entry}.
 * <p>
 * Implementations should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InputSocket
 * @author Christian Schlichtherle
 */
public interface OutputSocket<E extends Entry> extends IoSocket<E> {

    /**
     * Returns a new output stream for writing bytes.
     * The returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @param  peer the nullable peer socket for copying entry contents.
     * @return A new output stream for writing bytes.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    OutputStream stream(@Nullable InputSocket<? extends Entry> peer)
    throws IOException;

    /**
     * <b>Optional operation:</b> Returns a new seekable byte channel for
     * writing bytes.
     * If this method is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is output, the returned channel
     * may not be able to position the file pointer or read data and any
     * attempt to do so may fail with a {@link NonReadableChannelException}.
     *
     * @param  peer the nullable peer socket for copying entry contents.
     * @return A new seekable byte channel for writing bytes.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    SeekableByteChannel channel(@Nullable InputSocket<? extends Entry> peer)
    throws IOException;
}
