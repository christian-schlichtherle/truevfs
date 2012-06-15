/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;

/**
 * Delegates all methods to another input socket.
 * 
 * @see    DelegatingOutputSocket
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class DelegatingInputSocket<T extends Entry>
extends AbstractInputSocket<T> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     * @throws IOException on any I/O error. 
     */
    protected abstract InputSocket<? extends T> socket()
    throws IOException;

    @Override
    public T target() throws IOException {
        return socket().target();
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return socket().stream(peer);
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return socket().channel(peer);
    }
}
