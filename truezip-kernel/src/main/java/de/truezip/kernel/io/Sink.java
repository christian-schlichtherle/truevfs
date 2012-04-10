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
 * A provider for output streams and (optionally) seekable byte channels.
 * 
 * @see    Source
 * @author Christian Schlichtherle
 */
//@CleanupObligation
public interface Sink {

    /**
     * Returns an output stream for writing bytes.
     * The returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return An output stream for writing bytes.
     * @throws IOException on any I/O error.
     * @throws IllegalStateException if this method has already been called
     *         and a new output stream cannot get created.
     */
    @CreatesObligation
    OutputStream stream() throws IOException;

    /**
     * <b>Optional operation:</b> Returns a seekable byte channel for
     * writing bytes.
     * If this method is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A seekable byte channel for writing bytes.
     * @throws IOException on any I/O error.
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @throws IllegalStateException if this method has already been called
     *         and a new seekable byte channel cannot get created.
     */
    @CreatesObligation
    SeekableByteChannel channel() throws IOException;
}
