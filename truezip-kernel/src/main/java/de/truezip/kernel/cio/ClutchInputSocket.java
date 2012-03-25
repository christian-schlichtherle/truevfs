/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input socket which obtains its delegate lazily and {@link #reset()}s it
 * upon any {@link Throwable}.
 *
 * @see    ClutchOutputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class ClutchInputSocket<E extends Entry>
extends DelegatingInputSocket<E> {
    @CheckForNull InputSocket<? extends E> delegate;

    @Override
    protected final InputSocket<? extends E> getDelegate() throws IOException {
        final InputSocket<? extends E> is = delegate;
        return null != is ? is : (delegate = getLazyDelegate());
    };

    /**
     * Returns the delegate socket for lazy initialization.
     * 
     * @return the delegate socket for lazy initialization.
     * @throws IOException on any I/O failure. 
     */
    protected abstract InputSocket<? extends E> getLazyDelegate()
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
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        try {
            return getBoundDelegate().newReadOnlyFile();
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
    public InputStream newInputStream() throws IOException {
        try {
            return getBoundDelegate().newInputStream();
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
