/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.InputShop;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link InputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SynchronizedInputStream extends DecoratingInputStream {

    /** The object to synchronize on. */
    protected final Object lock;

    /**
     * Constructs a new synchronized input stream.
     * This object will synchronize on itself.
     *
     * @param in the input stream to wrap in this decorator.
     * @deprecated This class exists to control concurrent access to a
     *             protected resource, e.g. an {@link InputShop}.
     *             So the lock should never be this object itself.
     *             
     */
    public SynchronizedInputStream(@Nullable InputStream in) {
        this(in, null);
    }

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedInputStream(
            final @Nullable InputStream in,
            final @CheckForNull Object lock) {
        super(in);
        this.lock = null != lock ? lock : this;
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            return delegate.read();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            return delegate.read(b, off, len);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            return delegate.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (lock) {
            return delegate.available();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }

    @Override
    public void mark(int readlimit) {
        synchronized (lock) {
            delegate.mark(readlimit);
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            delegate.reset();
        }
    }

    @Override
    public boolean markSupported() {
        synchronized (lock) {
            return delegate.markSupported();
        }
    }
}
