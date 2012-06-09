/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
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
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock();

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
            public E target() throws IOException {
                lock.lock();
                try {
                    return socket().target();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            @GuardedBy("lock")
            public InputStream stream(OutputSocket<? extends Entry> peer)
            throws IOException {
                final InputStream in;
                lock.lock();
                try {
                    in = socket().stream(peer);
                } finally {
                    lock.unlock();
                }
                return new LockInputStream(in);
            }

            @Override
            @GuardedBy("lock")
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
            throws IOException {
                final SeekableByteChannel channel;
                lock.lock();
                try {
                    channel = socket().channel(peer);
                } finally {
                    lock.unlock();
                }
                return new LockSeekableChannel(channel);
            }
        } // Input

        return new Input();
    }

    private final class LockInputStream
    extends net.truevfs.kernel.io.LockInputStream {
        LockInputStream(@WillCloseWhenClosed InputStream in) {
            super(lock, in);
        }
    } // LockInputStream

    private final class LockSeekableChannel
    extends net.truevfs.kernel.io.LockSeekableChannel {
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(lock, channel);
        }
    } // LockSeekableChannel
}
