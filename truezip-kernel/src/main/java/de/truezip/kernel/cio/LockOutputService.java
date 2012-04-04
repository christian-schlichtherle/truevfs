/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.LockOutputStream;
import de.truezip.kernel.sbc.LockSeekableByteChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another output service to allow concurrent access which is
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries in the decorated output service.
 * @see    LockInputService
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new lock output service.
     * 
     * @param output the service to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockOutputService(@WillCloseWhenClosed OutputService<E> output) {
        this(output, new ReentrantLock());
    }

    /**
     * Constructs a new concurrent output service.
     * 
     * @param output the service to decorate.
     * @param lock The lock to use. 
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockOutputService(
            final @WillCloseWhenClosed OutputService<E> output,
            final Lock lock) {
        super(output);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    @GuardedBy("lock")
    public void close() throws IOException {
        lock.lock();
        try {
            container.close();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public @CheckForNull E getEntry(String name) {
        lock.lock();
        try {
            return container.getEntry(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public int size() {
        lock.lock();
        try {
            return container.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("The returned iterator would not be thread-safe!");
    }

    @Override
    public OutputSocket<E> getOutputSocket(final E entry) {
        final class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(container.getOutputSocket(entry));
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
            @GuardedBy("lock")
            public OutputStream newStream() throws IOException {
                final OutputStream out;
                lock.lock();
                try {
                    out = getBoundSocket().newStream();
                } finally {
                    lock.unlock();
                }
                return new LockOutputStream(out, lock);
            }

            @Override
            @GuardedBy("lock")
            public SeekableByteChannel newChannel() throws IOException {
                final SeekableByteChannel sbc;
                lock.lock();
                try {
                    sbc = getBoundSocket().newChannel();
                } finally {
                    lock.unlock();
                }
                return new LockSeekableByteChannel(sbc, lock);
            }
        } // Output

        return new Output();
    }
}
