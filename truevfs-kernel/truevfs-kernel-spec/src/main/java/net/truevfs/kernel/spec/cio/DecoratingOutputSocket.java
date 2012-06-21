/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * An abstract decorator for an output socket.
 * 
 * @see    DecoratingInputSocket
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputSocket<T extends Entry>
extends DelegatingOutputSocket<T> {

    /** The nullable decorated output socket. */
    protected @Nullable OutputSocket<? extends T> socket;

    protected DecoratingOutputSocket() { }

    protected DecoratingOutputSocket(final OutputSocket<? extends T> socket) {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    protected OutputSocket<? extends T> socket() throws IOException {
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
