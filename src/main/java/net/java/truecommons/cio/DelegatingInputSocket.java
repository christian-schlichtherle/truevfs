/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * Delegates all methods to another input socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    DelegatingOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class DelegatingInputSocket<E extends Entry>
extends AbstractInputSocket<E> {

    /**
     * Returns the delegate input socket.
     *
     * @return The delegate input socket.
     * @throws IOException on any I/O error.
     */
    protected abstract InputSocket<? extends E> socket() throws IOException;

    @Override
    public E target() throws IOException {
        return socket().target();
    }

    @Override
    public InputStream stream(@Nullable OutputSocket<? extends Entry> peer)
    throws IOException {
        return socket().stream(peer);
    }

    @Override
    public SeekableByteChannel channel(
            @Nullable OutputSocket<? extends Entry> peer)
    throws IOException {
        return socket().channel(peer);
    }
}
