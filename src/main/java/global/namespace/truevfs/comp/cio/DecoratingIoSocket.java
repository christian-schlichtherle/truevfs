/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.util.Objects;

/**
 * An abstract decorator for an I/O socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingIoSocket<E extends Entry, S extends IoSocket<? extends E>>
        implements DelegatingIoSocket<E> {

    /**
     * The decorated socket.
     */
    protected S socket;

    protected DecoratingIoSocket() {
    }

    protected DecoratingIoSocket(final S socket) {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    public S getSocket() {
        return socket;
    }
}
