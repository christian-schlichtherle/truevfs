/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * A factory for input resources for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. any input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 *
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @see    OutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public interface InputSocket<T extends Entry> extends IoSocket<T> {

    /**
     * Returns a new input stream for reading bytes.
     * The returned input stream should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     *
     * @param  peer the nullable peer socket for copying entry contents.
     * @return A new input stream for reading bytes.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    InputStream stream(@CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException;

    /**
     * <b>Optional operation:</b> Returns a new seekable byte channel for
     * reading bytes.
     * If this operation is supported, then the returned seekable byte channel
     * should <em>not</em> be buffered.
     * Buffering should get addressed by the caller instead.
     * <p>
     * Because the intention of this interface is input, the returned channel
     * may not be able to write data and any attempt to do so should fail with
     * a {@link NonWritableChannelException}.
     *
     * @param  peer the nullable peer socket for copying entry contents.
     * @return A new seekable byte channel for reading bytes.
     * @throws UnsupportedOperationException if this operation is not supported.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    SeekableByteChannel channel(@CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException;
}
