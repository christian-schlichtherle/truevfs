/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

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
extends IoSocket<E, Entry, OutputSocket<E>, InputSocket<? extends Entry>>, Sink {

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
