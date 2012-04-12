/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A provider for input streams or seekable byte channels.
 * 
 * @see    Sink
 * @author Christian Schlichtherle
 */
public interface Source {

    /**
     * Returns an input stream for reading bytes.
     * The returned input stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return An input stream for reading bytes.
     * @throws IOException on any I/O error.
     * @throws IllegalStateException if another input stream is not available.
     */
    @CreatesObligation
    InputStream stream() throws IOException;    

    /**
     * <b>Optional operation:</b> Returns a seekable byte channel for
     * reading bytes.
     * If this operation is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is input, the returned channel
     * may not be able to write data and any attempt to do so may fail with a
     * {@link NonWritableChannelException}.
     *
     * @return A seekable byte channel for reading bytes.
     * @throws IOException on any I/O error.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IllegalStateException if another seekable byte channel is not
     *         available.
     */
    @CreatesObligation
    SeekableByteChannel channel() throws IOException;
}
