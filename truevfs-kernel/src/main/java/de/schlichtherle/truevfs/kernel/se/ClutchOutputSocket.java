/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.cio.DecoratingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;

/**
 * An output lazySocket which obtains its delegate lazySocket lazily and
 * {@link #reset()}s it upon any {@link Throwable}.
 *
 * @see    ClutchInputSocket
 * @param  <E> the type of the {@link #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
abstract class ClutchOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    @Override
    protected final OutputSocket<? extends E> socket() throws IOException {
        final OutputSocket<? extends E> socket = this.socket;
        return null != socket ? socket : (this.socket = lazySocket());
    };

    /**
     * Returns the output lazySocket for lazy initialization.
     * 
     * @return the output lazySocket for lazy initialization.
     * @throws IOException on any I/O error. 
     */
    protected abstract OutputSocket<? extends E> lazySocket()
    throws IOException;

    @Override
    public E localTarget() throws IOException {
        try {
            return boundSocket().localTarget();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public SeekableByteChannel channel()
    throws IOException {
        try {
            return boundSocket().channel();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public OutputStream stream() throws IOException {
        try {
            return boundSocket().stream();
        } catch (final Throwable ex) {
            reset();
            throw ex;
        }
    }

    protected final void reset() {
        this.socket = null;
    }
}