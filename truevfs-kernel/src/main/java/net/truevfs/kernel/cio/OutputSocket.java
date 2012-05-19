/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import net.truevfs.kernel.io.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;

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
extends IOSocket<E, Entry>, Sink {

    /**
     * {@inheritDoc}
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * {@linkplain #getPeerSocket connected} to another socket.
     */
    @Override
    @CheckForNull Entry peerTarget() throws IOException;

    /**
     * Returns the nullable peer socket to which this socket is connected for
     * copying.
     * 
     * @return The nullable peer socket to which this socket is connected for
     *         copying.
     */
    @CheckForNull InputSocket<? extends Entry> getPeerSocket();

    /**
     * Inherits the {@linkplain #getPeerSocket peer socket} from the given
     * output socket.
     *
     * @param  to the output socket from which to inherit the peer socket.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    OutputSocket<E> bind(OutputSocket<? extends Entry> to);

    /**
     * Connects this output socket to the given {@code peer} input socket.
     * This method shall change the peer output socket of the given peer input
     * socket to this output socket, too.
     *
     * @param  peer the nullable peer input socket to connect to.
     * @return {@code this}
     */
    OutputSocket<E> connect(@CheckForNull InputSocket<? extends Entry> peer);

    /**
     * {@inheritDoc}
     * 
     * @return A <em>new</em> output stream for writing bytes.
     */
    @Override
    OutputStream stream() throws IOException;

    /**
     * {@inheritDoc}
     * 
     * @return A <em>new</em> seekable byte channel for writing bytes.
     */
    @Override
    SeekableByteChannel channel() throws IOException;
}
