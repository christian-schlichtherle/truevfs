/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link InputShop}, via a {@link Lock} object.
 *
 * @since   TrueZIP 7.4.4
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class LockReadOnlyFile extends DecoratingReadOnlyFile {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockReadOnlyFile(
            final @Nullable ReadOnlyFile rof,
            final Lock lock) {
        super(rof);
        if (null == lock)
            throw new NullPointerException();
        this.lock = lock;
    }

    @Override
    public long length() throws IOException {
        lock.lock();
        try {
            return delegate.length();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        lock.lock();
        try {
            return delegate.getFilePointer();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        lock.lock();
        try { 
            delegate.seek(pos);
        } finally {
            lock.unlock();
        }
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
    public void readFully(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            delegate.readFully(b, off, len);
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
}
