/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates all methods to another output socket.
 *
 * @see    DelegatingInputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DelegatingOutputSocket<E extends Entry>
extends OutputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     * @throws IOException On any I/O failure. 
     */
    protected abstract OutputSocket<? extends E> getDelegate()
    throws IOException;

    /**
     * Binds the delegate socket to this socket and returns it.
     *
     * @return The bound delegate socket.
     * @throws IOException On any I/O failure. 
     */
    // TODO: Consider declaring this final!
    protected OutputSocket<? extends E> getBoundDelegate() throws IOException {
        return getDelegate().bind(this);
    }

    @Override
    public E getLocalTarget() throws IOException {
        return getBoundDelegate().getLocalTarget();
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return getBoundDelegate().newSeekableByteChannel();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getBoundDelegate().newOutputStream();
    }
}
