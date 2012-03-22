/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.OutputShop;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link OutputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedInputStream
 * @deprecated Use {@link LockOutputStream} instead.
 * @author  Christian Schlichtherle
 */
@Deprecated
@ThreadSafe
public class SynchronizedOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on. */
    protected final Object lock;

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out the output stream to wrap in this decorator.
     * @deprecated This class exists to control concurrent access to a
     *             protected resource, e.g. an {@link OutputShop}.
     *             So the lock should never be this object itself.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedOutputStream(@Nullable @WillCloseWhenClosed OutputStream out) {
    	this(out, null);
    }

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedOutputStream(
            final @Nullable @WillCloseWhenClosed OutputStream out,
            final @CheckForNull Object lock) {
        super(out);
        this.lock = null != lock ? lock : this;
    }

    @Override
    @GuardedBy("lock")
    public void write(int b) throws IOException {
        synchronized (lock) {
            delegate.write(b);
        }
    }

    @Override
    @GuardedBy("lock")
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.write(b, off, len);
        }
    }

    @Override
    @GuardedBy("lock")
    public void flush() throws IOException {
        synchronized (lock) {
            delegate.flush();
        }
    }

    @Override
    @GuardedBy("lock")
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}