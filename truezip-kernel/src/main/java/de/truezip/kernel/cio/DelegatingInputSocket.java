/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates all methods to another input socket.
 * 
 * @see    DelegatingOutputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DelegatingInputSocket<E extends Entry>
extends InputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     * @throws IOException on any I/O failure. 
     */
    protected abstract InputSocket<? extends E> getSocket()
    throws IOException;

    /**
     * Binds the delegate socket to this socket and returns it.
     *
     * @return The bound delegate socket.
     * @throws IOException on any I/O failure. 
     */
    protected final InputSocket<? extends E> getBoundSocket()
    throws IOException {
        return getSocket().bind(this);
    }

    @Override
    public E getLocalTarget() throws IOException {
        return getBoundSocket().getLocalTarget();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return getBoundSocket().newReadOnlyFile();
    }

    @Override
    public SeekableByteChannel newChannel() throws IOException {
        return getBoundSocket().newChannel();
    }

    @Override
    public InputStream newStream() throws IOException {
        return getBoundSocket().newStream();
    }
}