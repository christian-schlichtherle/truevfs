/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.io;

import global.namespace.truevfs.commons.shed.Operation;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 */
public abstract class CheckedCloseable implements Closeable {

    private final Closeable closeable;
    private boolean closed;

    protected CheckedCloseable(final Closeable closeable) {
        this.closeable = closeable;
    }

    public final boolean isOpen() {
        return !closed;
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

    public final <T> T checked(final Operation<T, ? extends IOException> op) throws IOException {
        if (!isOpen()) {
            throw newClosedStreamException();
        }
        return op.run();
    }

    protected abstract ClosedStreamException newClosedStreamException();
}
