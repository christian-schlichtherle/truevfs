/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A provider for output streams or seekable byte channels.
 * 
 * @see    Source
 * @author Christian Schlichtherle
 */
public interface Sink {

    /**
     * Returns an output stream for writing bytes.
     * The returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return An output stream for writing bytes.
     * @throws IOException on any I/O error.
     * @throws IllegalStateException if another output stream is not available.
     */
    @CreatesObligation
    OutputStream stream() throws IOException;

    /**
     * <b>Optional operation:</b> Returns a seekable byte channel for
     * writing bytes.
     * If this method is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is output, the returned channel
     * may not be able to position the file pointer or to do input and any
     * attempt to do so may fail with a {@link NonReadableChannelException}.
     * 
     * @return A seekable byte channel for writing bytes.
     * @throws IOException on any I/O error.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IllegalStateException if another seekable byte channel is not
     *         available.
     */
    @CreatesObligation
    SeekableByteChannel channel() throws IOException;
}
