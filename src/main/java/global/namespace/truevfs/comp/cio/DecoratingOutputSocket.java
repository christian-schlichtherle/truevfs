/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

/**
 * An abstract decorator for an output socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 * @see DecoratingInputSocket
 */
public abstract class DecoratingOutputSocket<E extends Entry>
        extends DecoratingIoSocket<E, OutputSocket<? extends E>> implements DelegatingOutputSocket<E> {

    protected DecoratingOutputSocket() {
    }

    protected DecoratingOutputSocket(OutputSocket<? extends E> socket) {
        super(socket);
    }
}
