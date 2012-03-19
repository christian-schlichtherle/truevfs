/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.InputShop;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link InputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedOutputStream
 * @deprecated Use {@link LockInputStream} instead.
 * @author  Christian Schlichtherle
 */
@Deprecated
@ThreadSafe
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
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedInputStream(@Nullable @WillCloseWhenClosed InputStream in) {
        this(in, null);
    }

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedInputStream(
            final @Nullable @WillCloseWhenClosed InputStream in,
            final @CheckForNull Object lock) {
        super(in);
        this.lock = null != lock ? lock : this;
    }

    @Override
    @GuardedBy("lock")
    public int read() throws IOException {
        synchronized (lock) {
            return delegate.read();
        }
    }

    @Override
    @GuardedBy("lock")
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            return delegate.read(b, off, len);
        }
    }

    @Override
    @GuardedBy("lock")
    public long skip(long n) throws IOException {
        synchronized (lock) {
            return delegate.skip(n);
        }
    }

    @Override
    @GuardedBy("lock")
    public int available() throws IOException {
        synchronized (lock) {
            return delegate.available();
        }
    }

    @Override
    @GuardedBy("lock")
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }

    @Override
    @GuardedBy("lock")
    public void mark(int readlimit) {
        synchronized (lock) {
            delegate.mark(readlimit);
        }
    }

    @Override
    @GuardedBy("lock")
    public void reset() throws IOException {
        synchronized (lock) {
            delegate.reset();
        }
    }

    @Override
    @GuardedBy("lock")
    public boolean markSupported() {
        synchronized (lock) {
            return delegate.markSupported();
        }
    }
}