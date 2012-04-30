/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.LockingStrategy.TIMED_LOCK;
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
 * synchronized by a {@link Lock} object provided to its constructor.
 *
 * @param  <E> the type of the entries in the decorated input service.
 * @see    LockOutputService
 * @see    LockManagement
 * @author Christian Schlichtherle
 */
@ThreadSafe
class LockInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    /** The lock on which this object synchronizes. */
    private final Lock lock = new ReentrantLock(true);

    /**
     * Constructs a new lock input service.
     *
     * @param input the service to decorate.
     */
    LockInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
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

        TIMED_LOCK.apply(lock, new Close());
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

        return TIMED_LOCK.apply(lock, new Entry());
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

        return TIMED_LOCK.apply(lock, new Size());
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
                final class LocalTarget implements IOOperation<E> {
                    @Override
                    public E call() throws IOException {
                        return getBoundSocket().localTarget();
                    }
                } // GetLocalTarget

                return TIMED_LOCK.apply(lock, new LocalTarget());
            }

            @Override
            @GuardedBy("lock")
            public InputStream stream() throws IOException {
                final class Stream implements IOOperation<InputStream> {
                    @Override
                    public InputStream call() throws IOException {
                        return getBoundSocket().stream();
                    }
                } // Stream

                return new LockInputStream(TIMED_LOCK.apply(lock, new Stream()));
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

                return new LockSeekableChannel(TIMED_LOCK.apply(lock, new Channel()));
            }
        } // Input

        return new Input();
    }

    void close(final Closeable closeable) throws IOException {
        final class Close implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                closeable.close();
                return null;
            }
        } // Close

        TIMED_LOCK.apply(lock, new Close());
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
    } // LockSeekableChannel*/
}
