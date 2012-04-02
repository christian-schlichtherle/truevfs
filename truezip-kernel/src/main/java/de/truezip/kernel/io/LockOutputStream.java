/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which protects all access to it via a {@link Lock}.
 *
 * @see     LockInputStream
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class LockOutputStream extends DecoratingOutputStream {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockOutputStream(
            final @Nullable @WillCloseWhenClosed OutputStream out,
            final Lock lock) {
        super(out);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    @GuardedBy("lock")
    public void write(int b) throws IOException {
        lock.lock();
        try {
            out.write(b);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            out.write(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public void flush() throws IOException {
        lock.lock();
        try {
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public void close() throws IOException {
        lock.lock();
        try {
            out.close();
        } finally {
            lock.unlock();
        }
    }
}