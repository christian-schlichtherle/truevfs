/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.io.Closeable;
import java.io.IOException;

/**
 * A skeleton for an auto-closeable resource.
 *
 * @param  <X> The exception type which may get thrown by {@link #close()}.
 *         If this is an {@link IOException}, then the subclass can implement
 *         the {@link Closeable} interface, too.
 * @author Christian Schlichtherle
 */
public abstract class Resource<X extends Exception> implements AutoCloseable {

    private boolean closed;

    /**
     * Returns {@code true} if and only if this resource hasn't been
     *         {@linkplain #close() closed} yet.
     */
    public boolean isOpen() { return !closed; }

    /**
     * Closes this resource.
     * If this resource has already been closed, then the method returns
     * immediately.
     * Otherwise, the method {@link #onBeforeClose()} gets called.
     * Upon successful termination, this resource gets marked as closed.
     * Next, the method {@link #onAfterClose()} gets called.
     *
     * @throws X At the discretion of the methods {@link #onBeforeClose()} and
     *         {@link #onAfterClose()}.
     */
    @Override
    public void close() throws X {
        if (closed) return;
        onBeforeClose();
        closed = true;
        onAfterClose();
    }

    /**
     * A hook which gets called by {@link #close()} unless this resource has
     * already been closed.
     *
     * @throws X at the discretion of this method.
     *         Throwing an exception vetoes the closing of this resource.
     *         In this case, the hook may get called again later.
     */
    protected void onBeforeClose() throws X { }

    /**
     * A hook which gets called by {@link #close()} unless this resource has
     * already been closed and unless {@link #onBeforeClose()} throws an
     * exception.
     *
     * @throws X at the discretion of this method.
     *         Throwing an exception has no effect on the state of this resource.
     */
    protected void onAfterClose() throws X { }
}
