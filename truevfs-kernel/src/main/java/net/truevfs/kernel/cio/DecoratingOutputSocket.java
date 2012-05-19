/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator for an output socket.
 * 
 * @see    DecoratingInputSocket
 * @param  <E> the type of the {@link #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
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
        return String.format("%s[socket=%s]",
                getClass().getName(),
                socket);
    }
}
