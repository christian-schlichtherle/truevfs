/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.LockInputStream;
import de.schlichtherle.truezip.rof.LockReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another input shop to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @see     ConcurrentOutputShop
 * @param   <E> The type of the entries.
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private final Lock lock;

    /**
     * Constructs a concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    public ConcurrentInputShop(
            final InputShop<E> input,
            final Lock lock) {
        super(input);
        if (null == lock)
            throw new NullPointerException();
        this.lock = lock;
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
    public @CheckForNull E getEntry(String name) {
        lock.lock();
        try {
            return delegate.getEntry(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getSize() {
        lock.lock();
        try {
            return delegate.getSize();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("This returned iterator would not be thread-safe!");
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(ConcurrentInputShop.super.getInputSocket(name));
            }

            @Override
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public Entry getPeerTarget() throws IOException {
                lock.lock();
                try {
                    return getBoundSocket().getPeerTarget();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final ReadOnlyFile rof;
                lock.lock();
                try {
                    rof = getBoundSocket().newReadOnlyFile();
                } finally {
                    lock.unlock();
                }
                return new LockReadOnlyFile(rof, lock);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final InputStream in;
                lock.lock();
                try {
                    in = getBoundSocket().newInputStream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in,lock);
            }
        } // Input

        return new Input();
    }
}
