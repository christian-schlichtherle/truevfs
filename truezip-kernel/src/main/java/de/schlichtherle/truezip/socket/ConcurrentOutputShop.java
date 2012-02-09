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
import de.schlichtherle.truezip.io.LockOutputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another output shop to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @see     ConcurrentInputShop
 * @param   <E> The type of the entries.
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private final Lock lock;

    /**
     * Constructs a concurrent output shop.
     * 
     * @param output the shop to decorate.
     * @param lock The lock to use. 
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ConcurrentOutputShop(
            final @WillCloseWhenClosed OutputShop<E> output,
            final Lock lock) {
        super(output);
        if (null == lock)
            throw new NullPointerException();
        this.lock = lock;
    }

    @Override
    @GuardedBy("lock")
    public void close() throws IOException {
        lock.lock();
        try {
            delegate.close();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public @CheckForNull E getEntry(String name) {
        lock.lock();
        try {
            return delegate.getEntry(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
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
        throw new UnsupportedOperationException("The returned iterator would not be thread-safe!");
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(ConcurrentOutputShop.super.getOutputSocket(entry));
            }

            @Override
            @GuardedBy("lock")
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return entry;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            @GuardedBy("lock")
            public OutputStream newOutputStream() throws IOException {
                final OutputStream out;
                lock.lock();
                try {
                    out = getBoundSocket().newOutputStream();
                } finally {
                    lock.unlock();
                }
                return new LockOutputStream(out, lock);
            }
        } // Output

        return new Output();
    }
}
