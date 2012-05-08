/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.cio.DecoratingInputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input socket which obtains its delegate socket lazily and
 * {@link #reset()}s it upon any {@link Throwable}.
 *
 * @see    ClutchOutputSocket
 * @param  <E> the type of the {@link #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class ClutchInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    @Override
    protected final InputSocket<? extends E> getSocket() throws IOException {
        final InputSocket<? extends E> socket = this.socket;
        return null != socket ? socket : (this.socket = socket());
    };

    /**
     * Returns the input socket for lazy initialization.
     * 
     * @return the input socket for lazy initialization.
     * @throws IOException on any I/O error. 
     */
    protected abstract InputSocket<? extends E> socket() throws IOException;

    @Override
    public E localTarget() throws IOException {
        try {
            return getBoundSocket().localTarget();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public InputStream stream() throws IOException {
        try {
            return getBoundSocket().stream();
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

    protected final void reset() {
        this.socket = null;
    }
}