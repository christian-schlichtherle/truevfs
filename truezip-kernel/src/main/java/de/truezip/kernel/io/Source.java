/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * A provider for input streams and (optionally) seekable byte channels.
 * 
 * @see    Sink
 * @author Christian Schlichtherle
 */
@CleanupObligation
public interface Source {

    /**
     * Returns an input stream for reading bytes.
     * The returned input stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return An input stream for reading bytes.
     * @throws IOException on any I/O error.
     * @throws IllegalStateException if this method has already been called
     *         and a new input stream cannot get created.
     */
    @CreatesObligation
    InputStream stream() throws IOException;    

    /**
     * <b>Optional operation:</b> Returns a seekable byte channel for
     * reading bytes.
     * If this operation is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A seekable byte channel for reading bytes.
     * @throws IOException on any I/O error.
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @throws IllegalStateException if this method has already been called
     *         and a new seekable byte channel cannot get created.
     */
    @CreatesObligation
    SeekableByteChannel channel() throws IOException;
}
