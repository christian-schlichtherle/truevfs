/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.LockInputStream;
import de.schlichtherle.truezip.rof.LockReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another input shop to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries.
 * @see    LockOutputShop
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockInputShop(@WillCloseWhenClosed InputShop<E> input) {
        this(input, new ReentrantLock());
    }

    /**
     * Constructs a new concurrent input shop.
     *
     * @param input the shop to decorate.
     * @param lock The lock to use. 
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockInputShop(
            final @WillCloseWhenClosed InputShop<E> input,
            final Lock lock) {
        super(input);
        if (null == (this.lock = lock))
            throw new NullPointerException();
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
        throw new UnsupportedOperationException("This returned iterator would not be thread-safe!");
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(LockInputShop.super.getInputSocket(name));
            }

            @Override
            @GuardedBy("lock")
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            @GuardedBy("lock")
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
            @GuardedBy("lock")
            public InputStream newInputStream() throws IOException {
                final InputStream in;
                lock.lock();
                try {
                    in = getBoundSocket().newInputStream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in, lock);
            }
        } // Input

        return new Input();
    }
}
