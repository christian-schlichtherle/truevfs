/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which protects all access to its output stream
 * via a {@link Lock}.
 *
 * @see    LockInputStream
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockOutputStream extends DecoratingOutputStream {

    /** The lock on which this object synchronizes. */
    private final Lock lock;

    protected LockOutputStream(final Lock lock) {
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    public LockOutputStream(
            final Lock lock,
            final @WillCloseWhenClosed OutputStream out) {
        this(lock);
        if (null == (this.out = out))
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
    @DischargesObligation
    public void close() throws IOException {
        lock.lock();
        try {
            out.close();
        } finally {
            lock.unlock();
        }
    }
}
