/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.LockInputStream;
import de.truezip.kernel.io.LockSeekableChannel;
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
 * @param  <E> the type of the entries in the decorated input service.
 * @see    FsLockOutputService
 * @author Christian Schlichtherle
 */
@ThreadSafe
class LockInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock();

    /**
     * Constructs a new lock input service.
     *
     * @param input the service to decorate.
     */
    @CreatesObligation
    LockInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
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
    public InputSocket<E> getInputSocket(final String name) {
        final class Input extends DecoratingInputSocket<E> {
            Input() {
                super(container.getInputSocket(name));
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
            public InputStream stream() throws IOException {
                final InputStream in;
                lock.lock();
                try {
                    in = getBoundSocket().stream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in, lock);
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
        } // Input

        return new Input();
    }
}