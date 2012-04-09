/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.LockOutputStream;
import de.truezip.kernel.io.LockSeekableChannel;
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
 * @see    FsLockInputService
 * @author Christian Schlichtherle
 */
@ThreadSafe
class LockOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock();

    /**
     * Constructs a new lock output service.
     * 
     * @param output the service to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    LockOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
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
            public OutputStream stream() throws IOException {
                final OutputStream out;
                lock.lock();
                try {
                    out = getBoundSocket().stream();
                } finally {
                    lock.unlock();
                }
                return new LockOutputStream(out, lock);
            }

            @Override
            @GuardedBy("lock")
            public SeekableByteChannel channel() throws IOException {
                final SeekableByteChannel channel;
                lock.lock();
                try {
                    channel = getBoundSocket().channel();
                } finally {
                    lock.unlock();
                }
                return new LockSeekableChannel(channel, lock);
            }
        } // Output

        return new Output();
    }
}
