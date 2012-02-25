/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates all methods to another input socket.
 * 
 * @see    DelegatingOutputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DelegatingInputSocket<E extends Entry>
extends InputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     * @throws IOException On any I/O failure. 
     */
    protected abstract InputSocket<? extends E> getDelegate()
    throws IOException;

    /**
     * Binds the decorated socket to this socket and returns it.
     *
     * @return The bound delegate socket.
     * @throws IOException On any I/O failure. 
     */
    // TODO: Rename this to getBoundDelegate()
    protected InputSocket<? extends E> getBoundSocket() throws IOException {
        return getDelegate().bind(this);
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
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return getBoundSocket().newSeekableByteChannel();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return getBoundSocket().newInputStream();
    }
}
