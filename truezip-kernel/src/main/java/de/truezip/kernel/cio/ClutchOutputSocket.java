/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output socket which obtains its socket lazily and {@link #reset()}s it
 * upon any {@link Throwable}.
 *
 * @see    ClutchInputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class ClutchOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    public ClutchOutputSocket() {
        super(null);
    }

    @Override
    protected final OutputSocket<? extends E> getSocket() throws IOException {
        final OutputSocket<? extends E> socket = this.socket;
        return null != socket ? socket : (this.socket = getLazyDelegate());
    };

    /**
     * Returns the socket socket for lazy initialization.
     * 
     * @return the socket socket for lazy initialization.
     * @throws IOException on any I/O error. 
     */
    protected abstract OutputSocket<? extends E> getLazyDelegate()
    throws IOException;

    @Override
    public E getLocalTarget() throws IOException {
        try {
            return getBoundSocket().getLocalTarget();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public SeekableByteChannel channel()
    throws IOException {
        try {
            return getBoundSocket().channel();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public OutputStream stream() throws IOException {
        try {
            return getBoundSocket().stream();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    protected final void reset() {
        this.socket = null;
    }
}