/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.IOException;

/**
 * An abstract closeable resource.
 * 
 * @param  <X> The exception type which may get thrown by {@link #close()}.
 *         If this is an {@link IOException}, then the subclass can implement
 *         the {@link Closeable} interface, too.
 * @author Christian Schlichtherle
 */
@CleanupObligation
public abstract class Resource<X extends Exception> implements AutoCloseable {
    private boolean closed;

    /**
     * Closes this resource.
     * If this is the first call to this method, then {@link #onClose()} gets
     * called.
     * Otherwhise the call gets ignored.
     * Upon successful return from {@code onClose()}, this resource gets marked
     * as closed, so a subsequent call to this method will do nothing.
     * 
     * @throws X At the discretion of the method {@link #onClose()}.
     */
    @Override
    @DischargesObligation
    public final void close() throws X {
        if (!closed) {
            onClose();
            closed = true;
        }
    }

    protected abstract void onClose() throws X;
}