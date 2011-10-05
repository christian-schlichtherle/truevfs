/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * Delegates all methods to another input socket.
 * 
 * @see     DelegatingOutputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class DelegatingInputSocket<E extends Entry>
extends InputSocket<E> {

    /**
     * Returns the delegate socket.
     * 
     * @return The delegate socket.
     */
    protected abstract InputSocket<? extends E> getDelegate()
    throws IOException;

    /**
     * Binds the decorated socket to this socket and returns it.
     *
     * @return The bound decorated socket.
     */
    protected InputSocket<? extends E> getBoundSocket() throws IOException {
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
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return getBoundSocket().newReadOnlyFile();
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return getBoundSocket().newSeekableByteChannel();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return getBoundSocket().newInputStream();
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
