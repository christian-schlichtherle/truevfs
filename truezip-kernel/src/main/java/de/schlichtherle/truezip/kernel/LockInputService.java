/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.cio.*;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
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
 * synchronized by a private {@link Lock} object.
 *
 * @param  <E> the type of the entries in the decorated input service.
 * @see    LockOutputService
 * @see    LockManagement
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock();

    /**
     * Constructs a new lock input service.
     *
     * @param input the service to decorate.
     */
    public LockInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    @Override
    @GuardedBy("lock")
    @DischargesObligation
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
    public @CheckForNull E entry(final String name) {
        lock.lock();
        try {
            return container.entry(name);
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
    public InputSocket<E> input(final String name) {
        final class Input extends DecoratingInputSocket<E> {
            Input() {
                super(container.input(name));
            }

            @Override
            @GuardedBy("lock")
            public E localTarget() throws IOException {
                lock.lock();
                try {
                    return boundSocket().localTarget();
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
                    in = boundSocket().stream();
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in);
            }

            @Override
            @GuardedBy("lock")
            public SeekableByteChannel channel() throws IOException {
                final SeekableByteChannel channel;
                lock.lock();
                try {
                    channel = boundSocket().channel();
                } finally {
                    lock.unlock();
                }
                return new LockSeekableChannel(channel);
            }
        } // Input

        return new Input();
    }

    void close(final Closeable closeable) throws IOException {
        lock.lock();
        try {
            closeable.close();
        } finally {
            lock.unlock();
        }
    }

    private final class LockInputStream
    extends de.truezip.kernel.io.LockInputStream {
        LockInputStream(@WillCloseWhenClosed InputStream in) {
            super(lock, in);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(in);
        }
    } // LockInputStream

    private final class LockSeekableChannel
    extends de.truezip.kernel.io.LockSeekableChannel {
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(lock, channel);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(channel);
        }
    } // LockSeekableChannel
}
