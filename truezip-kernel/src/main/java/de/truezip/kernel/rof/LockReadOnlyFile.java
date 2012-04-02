/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator which protects all access to a shared resource via a {@link Lock} object.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class LockReadOnlyFile extends DecoratingReadOnlyFile {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockReadOnlyFile(
            final @Nullable @WillCloseWhenClosed ReadOnlyFile rof,
            final Lock lock) {
        super(rof);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    public long length() throws IOException {
        lock.lock();
        try {
            return rof.length();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        lock.lock();
        try {
            return rof.getFilePointer();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        lock.lock();
        try { 
            rof.seek(pos);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            return rof.read();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            return rof.read(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            rof.readFully(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            rof.close();
        } finally {
            lock.unlock();
        }
    }
}