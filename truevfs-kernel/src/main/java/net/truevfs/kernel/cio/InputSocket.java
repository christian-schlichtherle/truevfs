/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import net.truevfs.kernel.io.Source;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;

/**
 * A factory for input resources for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. any input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 *
 * @param  <E> the type of the {@link #localTarget() local target}
 *         for I/O operations.
 * @see    OutputSocket
 * @author Christian Schlichtherle
 */
public interface InputSocket<E extends Entry>
extends IOSocket<E, Entry>, Source {

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
    @CheckForNull OutputSocket<? extends Entry> getPeerSocket();

    /**
     * Inherits the {@linkplain #getPeerSocket peer socket} from the given
     * input socket.
     *
     * @param  to the input socket from which to inherit the peer socket.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    InputSocket<E> bind(InputSocket<? extends Entry> to);

    /**
     * Connects this input socket to the given {@code peer} output socket.
     * This method shall change the peer input socket of the given peer output
     * socket to this input socket, too.
     *
     * @param  peer the nullable peer output socket to connect to.
     * @return {@code this}
     */
    InputSocket<E> connect(@CheckForNull OutputSocket<? extends Entry> peer);

    /**
     * {@inheritDoc}
     * 
     * @return A <em>new</em> input stream for reading bytes.
     */
    @Override
    InputStream stream() throws IOException;

    /**
     * {@inheritDoc}
     * 
     * @return A <em>new</em> seekable byte channel for reading bytes.
     */
    @Override
    SeekableByteChannel channel() throws IOException;
}
