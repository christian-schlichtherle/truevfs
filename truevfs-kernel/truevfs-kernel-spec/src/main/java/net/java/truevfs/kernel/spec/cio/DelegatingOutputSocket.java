/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Delegates all methods to another output socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    DelegatingInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class DelegatingOutputSocket<E extends Entry>
extends AbstractOutputSocket<E> {

    /**
     * Returns the delegate output socket.
     * 
     * @return The delegate output socket.
     * @throws IOException on any I/O error. 
     */
    protected abstract OutputSocket<? extends E> socket() throws IOException;

    @Override
    public E target() throws IOException {
        return socket().target();
    }

    @Override
    public OutputStream stream(@CheckForNull InputSocket<? extends Entry> peer)
    throws IOException {
        return socket().stream(peer);
    }

    @Override
    public SeekableByteChannel channel(
            @CheckForNull InputSocket<? extends Entry> peer)
    throws IOException {
        return socket().channel(peer);
    }
}
