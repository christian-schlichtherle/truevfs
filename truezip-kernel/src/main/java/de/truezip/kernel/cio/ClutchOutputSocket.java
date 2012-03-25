/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.cio.Entry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output socket which obtains its delegate lazily and {@link #reset()}s it
 * upon any {@link Throwable}.
 *
 * @see    ClutchInputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class ClutchOutputSocket<E extends Entry>
extends DelegatingOutputSocket<E> {
    @CheckForNull OutputSocket<? extends E> delegate;

    @Override
    protected final OutputSocket<? extends E> getDelegate() throws IOException {
        final OutputSocket<? extends E> os = delegate;
        return null != os ? os : (delegate = getLazyDelegate());
    };

    /**
     * Returns the delegate socket for lazy initialization.
     * 
     * @return the delegate socket for lazy initialization.
     * @throws IOException on any I/O failure. 
     */
    protected abstract OutputSocket<? extends E> getLazyDelegate()
    throws IOException;

    @Override
    public E getLocalTarget() throws IOException {
        try {
            return getBoundDelegate().getLocalTarget();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel()
    throws IOException {
        try {
            return getBoundDelegate().newSeekableByteChannel();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        try {
            return getBoundDelegate().newOutputStream();
        } catch (Throwable ex) {
            throw reset(ex);
        }
    }

    private IOException reset(final Throwable ex) {
        reset();
        if (ex instanceof RuntimeException)
            throw (RuntimeException) ex;
        else if (ex instanceof Error)
            throw (Error) ex;
        return (IOException) ex;
    }

    protected final void reset() {
        this.delegate = null;
    }
}