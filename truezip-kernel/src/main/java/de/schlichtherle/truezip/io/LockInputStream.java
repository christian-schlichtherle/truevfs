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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link InputShop}, via a {@link Lock} object.
 *
 * @see     LockOutputStream
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class LockInputStream extends DecoratingInputStream {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockInputStream(
            final @Nullable InputStream in,
            final Lock lock) {
        super(in);
        if (null == lock)
            throw new NullPointerException();
        this.lock = lock;
    }

    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            return delegate.read();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            return delegate.read(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        lock.lock();
        try {
            return delegate.skip(n);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int available() throws IOException {
        lock.lock();
        try {
            return delegate.available();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            delegate.close();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void mark(int readlimit) {
        lock.lock();
        try {
            delegate.mark(readlimit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() throws IOException {
        lock.lock();
        try {
            delegate.reset();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean markSupported() {
        lock.lock();
        try {
            return delegate.markSupported();
        } finally {
            lock.unlock();
        }
    }
}
