/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * A provider for output streams or (optionally) seekable byte channels.
 * 
 * @author Christian Schlichtherle
 */
//@CleanupObligation
public interface Sink {

    /**
     * <b>Optional operation:</b> Returns a new seekable byte channel for
     * writing bytes in random order.
     * <p>
     * If this method is supported, the implementation must enable calling it
     * any number of times.
     * Furthermore, the returned seekable byte channel should <em>not</em> be
     * buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A new seekable byte channel.
     * @throws IOException on any I/O failure.
     * @throws UnsupportedOperationException if this operation is not supported
     * by the implementation.
     */
    @CreatesObligation
    SeekableByteChannel newSeekableByteChannel() throws IOException;

    /**
     * Returns a new output stream for writing bytes.
     * <p>
     * The implementation must enable calling this method any number of times.
     * Furthermore, the returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A new output stream.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    OutputStream newOutputStream() throws IOException;
}
