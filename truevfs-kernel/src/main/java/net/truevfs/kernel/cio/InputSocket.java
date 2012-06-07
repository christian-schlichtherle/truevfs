/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import net.truevfs.kernel.io.Source;

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
extends IoSocket<E, Entry, InputSocket<E>, OutputSocket<? extends Entry>>, Source {

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
