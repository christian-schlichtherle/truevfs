/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import lombok.SneakyThrows;
import net.java.truecommons.io.ClosedStreamException;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
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
    @DischargesObligation
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
