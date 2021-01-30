/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.cio;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract decorator for an output socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    DecoratingInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class DecoratingOutputSocket<E extends Entry>
extends DelegatingOutputSocket<E> {

    /** The nullable decorated output socket. */
    protected @Nullable OutputSocket<? extends E> socket;

    protected DecoratingOutputSocket() { }

    protected DecoratingOutputSocket(final OutputSocket<? extends E> socket) {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    protected OutputSocket<? extends E> socket() throws IOException {
        return socket;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[socket=%s]",
                getClass().getName(),
                hashCode(),
                socket);
    }
}
