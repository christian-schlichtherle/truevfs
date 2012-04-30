/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.LockingStrategy.DEAD_LOCK;
import de.truezip.kernel.cio.*;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
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
 * @see    LockManagement
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
    LockOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
    }

    @Override
    @GuardedBy("lock")
    @DischargesObligation
    public void close() throws IOException {
        final class Close implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                container.close();
                return null;
            }
        } // Close

        DEAD_LOCK.apply(lock, new Close());
    }

    @Override
    @GuardedBy("lock")
    public @CheckForNull E entry(final String name) {
        final class Entry implements Operation<E, RuntimeException> {
            @Override
            public E call() {
                return container.entry(name);
            }
        } // Entry

        return DEAD_LOCK.apply(lock, new Entry());
    }

    @Override
    @GuardedBy("lock")
    public int size() {
        final class Size implements Operation<Integer, RuntimeException> {
            @Override
            public Integer call() {
                return container.size();
            }
        } // Size

        return DEAD_LOCK.apply(lock, new Size());
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
                final class Stream implements IOOperation<OutputStream> {
                    @Override
                    public OutputStream call() throws IOException {
                        return getBoundSocket().stream();
                    }
                } // Stream

                return new LockOutputStream(DEAD_LOCK.apply(lock, new Stream()));
            }

            @Override
            @GuardedBy("lock")
            public SeekableByteChannel channel() throws IOException {
                final class Channel implements IOOperation<SeekableByteChannel> {
                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return getBoundSocket().channel();
                    }
                } // Channel

                return new LockSeekableChannel(DEAD_LOCK.apply(lock, new Channel()));
            }
        } // Output

        return new Output();
    }

    void close(final Closeable closeable) throws IOException {
        final class Close implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                closeable.close();
                return null;
            }
        } // Close

        DEAD_LOCK.apply(lock, new Close());
    }

    private final class LockOutputStream
    extends de.truezip.kernel.io.LockOutputStream {
        LockOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(lock, out);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(out);
        }
    } // LockOutputStream

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
