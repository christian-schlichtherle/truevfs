/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.LockInputStream;
import de.truezip.kernel.rof.LockReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
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
 * Decorates another input service to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries.
 * @see    LockOutputService
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new concurrent input service.
     *
     * @param input the service to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockInputService(@WillCloseWhenClosed InputService<E> input) {
        this(input, new ReentrantLock());
    }

    /**
     * Constructs a new concurrent input service.
     *
     * @param input the service to decorate.
     * @param lock The lock to use. 
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockInputService(
            final @WillCloseWhenClosed InputService<E> input,
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
    public int size() {
        lock.lock();
        try {
            return delegate.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("This returned iterator would not be thread-safe!");
    }

    @Override
    public InputSocket<E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(LockInputService.super.getInputSocket(name));
            }

            @Override
            @GuardedBy("lock")
            public E getLocalTarget() throws IOException {
                lock.lock();
                try {
                    return getBoundDelegate().getLocalTarget();
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
                    rof = getBoundDelegate().newReadOnlyFile();
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
                    in = getBoundDelegate().newInputStream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in, lock);
            }
        } // Input

        return new Input();
    }
}