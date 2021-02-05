/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import java.io.IOException;

/**
 * Forwards all calls to another I/O socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 */
public interface DelegatingIoSocket<E extends Entry> extends IoSocket<E> {

    /**
     * Returns the delegate I/O socket.
     */
    IoSocket<? extends E> getSocket();

    @Override
    default E getTarget() throws IOException {
        return getSocket().getTarget();
    }
}
