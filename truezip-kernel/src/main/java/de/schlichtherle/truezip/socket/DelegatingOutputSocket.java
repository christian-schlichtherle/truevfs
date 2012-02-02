/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates all methods to another output socket.
 *
 * @see     DelegatingInputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class DelegatingOutputSocket<E extends Entry>
extends OutputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     */
    protected abstract OutputSocket<? extends E> getDelegate()
    throws IOException;

    /**
     * Binds the delegate socket to this socket and returns it.
     *
     * @return The bound decorated socket.
     */
    protected OutputSocket<? extends E> getBoundSocket() throws IOException {
        return getDelegate().bind(this);
    }

    @Override
    public E getLocalTarget() throws IOException {
        return getBoundSocket().getLocalTarget();
    }

    @Override
    @Nullable
    public Entry getPeerTarget() throws IOException {
        return getBoundSocket().getPeerTarget();
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return getBoundSocket().newSeekableByteChannel();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getBoundSocket().newOutputStream();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }
}
