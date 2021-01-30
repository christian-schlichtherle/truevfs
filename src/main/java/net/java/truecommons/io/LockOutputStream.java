/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * A decorator which protects all access to its output stream
 * via a {@link Lock}.
 *
 * @see    LockInputStream
 * @author Christian Schlichtherle
 */
public class LockOutputStream extends DecoratingOutputStream {

    /** The lock on which this object synchronizes. */
    private final Lock lock;

    /**
     * Constructs a new lock output stream.
     * Closing this stream will close the given stream.
     *
     * @param lock the lock to use for synchronization.
     * @param out the stream to decorate.
     */
    public LockOutputStream(final Lock lock, final OutputStream out) {
        super(out);
        this.lock = Objects.requireNonNull(lock);
    }

    @Override
    public void write(int b) throws IOException {
        lock.lock();
        try {
            out.write(b);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            out.write(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        lock.lock();
        try {
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            out.close();
        } finally {
            lock.unlock();
        }
    }
}
