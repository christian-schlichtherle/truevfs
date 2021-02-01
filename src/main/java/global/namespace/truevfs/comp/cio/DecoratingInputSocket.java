/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

/**
 * An abstract decorator for an input socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 * @see DecoratingOutputSocket
 */
public abstract class DecoratingInputSocket<E extends Entry>
        extends DecoratingIoSocket<E, InputSocket<? extends E>> implements DelegatingInputSocket<E> {

    protected DecoratingInputSocket() {
    }

    protected DecoratingInputSocket(InputSocket<? extends E> socket) {
        super(socket);
    }
}
