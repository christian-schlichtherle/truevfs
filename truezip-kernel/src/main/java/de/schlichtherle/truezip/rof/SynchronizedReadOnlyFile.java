/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.socket.InputShop;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which synchronizes all access to a {@link ReadOnlyFile}
 * via an object provided to its constructor.
 *
 * @deprecated Use {@link LockReadOnlyFile} instead.
 * @author  Christian Schlichtherle
 */
@Deprecated
@ThreadSafe
public class SynchronizedReadOnlyFile extends DecoratingReadOnlyFile {

    /** The object to synchronize on. */
    protected final Object lock;

    /**
     * Constructs a new synchronized read only file.
     * This object will synchronize on itself.
     *
     * @param rof the read only file to wrap in this decorator.
     * @deprecated This class exists to control concurrent access to a
     *             protected resource, e.g. an {@link InputShop}.
     *             So the lock should never be this object itself.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedReadOnlyFile(
            final @Nullable @WillCloseWhenClosed ReadOnlyFile rof) {
        this(rof, null);
    }

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedReadOnlyFile(
            final @Nullable @WillCloseWhenClosed ReadOnlyFile rof,
            final @CheckForNull Object lock) {
        super(rof);
        this.lock = null != lock ? lock : this;
    }

    @Override
    public long length() throws IOException {
        synchronized (lock) {
            return delegate.length();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        synchronized (lock) {
            return delegate.getFilePointer();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        synchronized (lock) {
            delegate.seek(pos);
        }
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
    public void readFully(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.readFully(b, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}