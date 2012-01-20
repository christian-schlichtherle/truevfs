/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link OutputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SynchronizedOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on - never {@code null}. */
    protected final Object lock;

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out the output stream to wrap in this decorator.
     */
    public SynchronizedOutputStream(@Nullable OutputStream out) {
    	this(out, null);
    }

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedOutputStream(
            final @Nullable OutputStream out,
            final @CheckForNull Object lock) {
        super(out);
        this.lock = null != lock ? lock : this;
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (lock) {
            delegate.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            delegate.flush();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}
