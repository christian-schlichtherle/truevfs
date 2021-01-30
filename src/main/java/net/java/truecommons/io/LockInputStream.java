/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * A decorator which protects all access to its input stream
 * via a {@link Lock}.
 *
 * @see    LockOutputStream
 * @author Christian Schlichtherle
 */
public class LockInputStream extends DecoratingInputStream {

    /** The lock on which this object synchronizes. */
    private final Lock lock;

    /**
     * Constructs a new lock input stream.
     * Closing this stream will close the given stream.
     *
     * @param lock the lock to use for synchronization.
     * @param in the stream to decorate.
     */
    public LockInputStream(final Lock lock, final InputStream in) {
        super(in);
        this.lock = Objects.requireNonNull(lock);
    }

    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            return in.read();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            return in.read(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        lock.lock();
        try {
            return in.skip(n);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int available() throws IOException {
        lock.lock();
        try {
            return in.available();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void mark(int readlimit) {
        lock.lock();
        try {
            in.mark(readlimit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() throws IOException {
        lock.lock();
        try {
            in.reset();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean markSupported() {
        lock.lock();
        try {
            return in.markSupported();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            in.close();
        } finally {
            lock.unlock();
        }
    }
}
