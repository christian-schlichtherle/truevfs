/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.truevfs.kernel.io.Sink;

/**
 * A factory for output resources for writing bytes to its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between output sockets and input sockets
 * is n:1, i.e. any output socket can have at most one peer input socket, but
 * it may be the peer of many other input sockets.
 *
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @see    InputSocket
 * @author Christian Schlichtherle
 */
public interface OutputSocket<E extends Entry>
extends IoSocket<E, Entry, OutputSocket<E>, InputSocket<? extends Entry>> {

    /**
     * Returns a new output stream for writing bytes.
     * The returned output stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @return A new output stream for writing bytes.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    OutputStream stream() throws IOException;

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
     * @return A new seekable byte channel for writing bytes.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    SeekableByteChannel channel() throws IOException;
}
