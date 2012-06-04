/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
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
 * synchronized by a private {@link Lock} object.
 *
 * @param  <E> the type of the entries in the decorated output service.
 * @see    LockInputService
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock();

    public LockOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
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
    public OutputSocket<E> output(final E entry) {
        final class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(container.output(entry));
            }

            @Override
            public E localTarget() throws IOException {
                return entry;
            }

            @Override
            @GuardedBy("lock")
            public OutputStream stream() throws IOException {
                final OutputStream in;
                lock.lock();
                try {
                    in = boundSocket().stream();
                } finally {
                    lock.unlock();
                }
                return new LockOutputStream(in);
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
        } // Output

        return new Output();
    }

    private final class LockOutputStream
    extends net.truevfs.kernel.io.LockOutputStream {
        LockOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(lock, out);
        }
    } // LockOutputStream

    private final class LockSeekableChannel
    extends net.truevfs.kernel.io.LockSeekableChannel {
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(lock, channel);
        }
    } // LockSeekableChannel
}
