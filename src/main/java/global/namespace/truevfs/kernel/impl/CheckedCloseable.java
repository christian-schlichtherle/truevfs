/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import lombok.SneakyThrows;
import global.namespace.truevfs.comp.io.ClosedStreamException;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 */
class CheckedCloseable implements Closeable {

    private final Closeable closeable;
    private boolean closed;

    CheckedCloseable(final Closeable closeable) {
        this.closeable = closeable;
    }

    /**
     * Closes this object.
     * Subsequent calls to this method will just forward the call to the delegate closeable.
     */
    @Override
    public final void close() throws IOException {
        closed = true;
        closeable.close();
    }

    public final boolean isOpen() { return !closed; }

    final void check() throws ClosedStreamException {
        if (!isOpen()) {
            throw newClosedStreamException();
        }
    }

    ClosedStreamException newClosedStreamException() {
        return new ClosedStreamException();
    }

    @SneakyThrows
    final <T, X extends Exception> T checked(final Op<T, X> op) throws X {
        check();
        return op.call();
    }

}
