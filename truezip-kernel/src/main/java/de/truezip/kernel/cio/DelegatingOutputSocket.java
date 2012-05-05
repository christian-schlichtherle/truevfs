/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates all methods to another output socket.
 *
 * @see    DelegatingInputSocket
 * @param  <E> the type of the {@link #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DelegatingOutputSocket<E extends Entry>
extends AbstractOutputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     * @throws IOException on any I/O error. 
     */
    protected abstract OutputSocket<? extends E> getSocket()
    throws IOException;

    /**
     * Binds the delegate socket to this socket and returns it.
     *
     * @return The bound delegate socket.
     * @throws IOException on any I/O error. 
     */
    protected final OutputSocket<? extends E> getBoundSocket()
    throws IOException {
        return getSocket().bind(this);
    }

    @Override
    public E localTarget() throws IOException {
        return getBoundSocket().localTarget();
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getBoundSocket().channel();
    }

    @Override
    public OutputStream stream() throws IOException {
        return getBoundSocket().stream();
    }
}