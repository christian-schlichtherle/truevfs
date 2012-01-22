/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.socket.OutputShop;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which protects all access to a shared resource, e.g. an
 * {@link OutputShop}, via a {@link Lock} object.
 *
 * @see     LockInputStream
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class LockOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on. */
    protected final Lock lock;

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     */
    public LockOutputStream(
            final @Nullable OutputStream out,
            final Lock lock) {
        super(out);
        if (null == lock)
            throw new NullPointerException();
        this.lock = lock;
    }

    @Override
    public void write(int b) throws IOException {
        lock.lock();
        try {
            delegate.write(b);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            delegate.write(b, off, len);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        lock.lock();
        try {
            delegate.flush();
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
