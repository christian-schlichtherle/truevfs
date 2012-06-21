/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * An abstract decorator for an input socket.
 * 
 * @see    DecoratingOutputSocket
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputSocket<T extends Entry>
extends DelegatingInputSocket<T> {

    /** The nullable decorated input socket. */
    protected @Nullable InputSocket<? extends T> socket;

    protected DecoratingInputSocket() { }

    protected DecoratingInputSocket(final InputSocket<? extends T> socket) {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    protected InputSocket<? extends T> socket() throws IOException {
        return socket;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[socket=%s]",
                getClass().getName(),
                socket);
    }
}
