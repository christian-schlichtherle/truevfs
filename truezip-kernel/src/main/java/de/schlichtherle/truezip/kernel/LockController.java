/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.*;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.FsSyncOption;
import static de.truezip.kernel.FsSyncOption.WAIT_CLOSE_IO;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import de.truezip.kernel.util.Threads;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides read/write locking for multi-threaded access by its clients.
 * 
 * @see    LockModel
 * @see    NeedsWriteLockException
 * @author Christian Schlichtherle
 */
@Immutable
final class LockController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private static final ThreadLocal<ThreadUtil>
            threadUtil = new ThreadLocal<ThreadUtil>() {
                @Override
                public ThreadUtil initialValue() {
                    return new ThreadUtil(ThreadLocalRandom.current());
                }
            };

    private static final BitField<FsSyncOption>
            NOT_WAIT_CLOSE_IO = BitField.of(WAIT_CLOSE_IO).not();

    private final ReadLock readLock;
    private final WriteLock writeLock;

    /**
     * Constructs a new file system lock controller.
     *
     * @param controller the decorated file system controller.
     */
    LockController(FsController<? extends LockModel> controller) {
        super(controller);
        this.readLock = getModel().readLock();
        this.writeLock = getModel().writeLock();
    }

    @Override
    ReadLock readLock() {
        return this.readLock;
    }

    @Override
    WriteLock writeLock() {
        return this.writeLock;
    }

    <T> T readOrWriteLocked(IOOperation<T> operation)
    throws IOException {
        try {
            return readLocked(operation);
        } catch (NeedsWriteLockException ex) {
            return writeLocked(operation);
        }
    }

    <T> T readLocked(IOOperation<T> operation) throws IOException {
        return locked(operation, readLock());
    }

    <T> T writeLocked(IOOperation<T> operation) throws IOException {
        assert !getModel().isReadLockedByCurrentThread()
                : "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!";
        return locked(operation, writeLock());
    }

    /**
     * Tries to call the given consistent operation while holding the given
     * lock.
     * <p>
     * If this is the first execution of this method on the call stack of the
     * current thread, then the lock gets acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason and the thrown exception chain contains a
     * {@link NeedsLockRetryException}, then the lock gets temporarily
     * released and the current thread gets paused for a small random time
     * interval before this procedure starts over again.
     * Otherwise, the exception chain gets just passed on to the caller.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call
     * stack of the current thread, then the lock gets acquired using
     * {@link Lock#tryLock()} instead.
     * If this fails, an {@code NeedsLockRetryException} gets created and
     * passed to the given exception handler for mapping before finally
     * throwing the resulting exception by executing
     * {@code throw handler.fail(new NeedsLockRetryException())}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason then the exception chain gets just passed
     * on to the caller.
     * <p>
     * This algorithm prevents dead locks effectively by temporarily unwinding
     * the stack and releasing all locks for a small random time interval.
     * Note that this requires some minimal cooperation by the operation:
     * Whenever it throws an exception, it MUST leave its resources in a
     * consistent state so that it can get retried again!
     * Mind that this is standard requirement for any {@link FsController}.
     * 
     * @param  <T> The return type of the operation.
     * @param  operation The atomic operation.
     * @param  lock The lock to hold while calling the operation.
     * @return The result of the operation.
     * @throws IOException As thrown by the operation.
     * @throws NeedsLockRetryException See above.
     */
    private <T> T locked(final IOOperation<T> operation, final Lock lock)
    throws IOException {
        final ThreadUtil thread = threadUtil.get();
        if (thread.locking) {
            if (!lock.tryLock())
                throw NeedsLockRetryException.get(getModel());
            try {
                return operation.call();
            } finally {
                lock.unlock();
            }
        } else {
            while (true) {
                try {
                    lock.lock();
                    thread.locking = true;
                    try {
                        return operation.call();
                    } finally {
                        thread.locking = false;
                        lock.unlock();
                    }
                } catch (NeedsLockRetryException ex) {
                    thread.pause();
                }
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return readOrWriteLocked(new IsReadOnly());
    }

    private final class IsReadOnly implements IOOperation<Boolean> {
        @Override
        public Boolean call() throws IOException {
            return controller.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        final class GetEntry implements IOOperation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return controller.getEntry(name);
            }
        } // GetEntry

        return readOrWriteLocked(new GetEntry());
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final class IsReadable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.isReadable(name);
            }
        } // IsReadable

        return readOrWriteLocked(new IsReadable());
    }
    
    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final class IsWritable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.isWritable(name);
            }
        } // IsWritable

        return readOrWriteLocked(new IsWritable());
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final class IsExecutable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.isExecutable(name);
            }
        } // IsExecutable

        return readOrWriteLocked(new IsExecutable());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final class SetReadOnly implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.setReadOnly(name);
                return null;
            }
        } // SetReadOnly

        writeLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.setTime(name, times, options);
            }
        } // SetTime

        return writeLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.setTime(name, types, value, options);
            }
        } // SetTime

        return writeLocked(new SetTime());
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.getInputSocket(name, options));
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                return writeLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public InputStream newStream() throws IOException {
                return writeLocked(new NewStream());
            }

            final class NewStream implements IOOperation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockInputStream(getBoundSocket().newStream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                return writeLocked(new NewChannel());
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableChannel(getBoundSocket().newChannel());
                }
            } // NewChannel
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.getOutputSocket(name, options, template));
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                return writeLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public OutputStream newStream() throws IOException {
                return writeLocked(new NewStream());
            }

            final class NewStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockOutputStream(getBoundSocket().newStream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                return writeLocked(new NewChannel());
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableChannel(getBoundSocket().newChannel());
                }
            } // NewChannel
        } // Output

        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void
    mknod(  final FsEntryName name,
            final Type type,
            final BitField<FsAccessOption> options,
            final Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.mknod(name, type, options, template);
                return null;
            }
        } // Mknod

        writeLocked(new Mknod());
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.unlink(name, options);
                return null;
            }
        } // Unlink

        writeLocked(new Unlink());
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        // MUST not initialize within IOOperation => would always be true!
        final BitField<FsSyncOption> sync = threadUtil.get().locking
                ? options.and(NOT_WAIT_CLOSE_IO) // may be == options!
                : options;

        final class Sync implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                // Prevent potential dead locks by performing a timed wait for
                // open I/O resources if the current thread is already holding
                // a file system lock.
                // Note that a sync in a parent file system is a rare event
                // so that this should not create performance problems, even
                // when accessing deeply nested archive files, e.g. for the
                // integration tests.
                try {
                    controller.sync(sync, handler);
                } catch (final FsSyncWarningException ex) {
                    throw ex; // may be FORCE_CLOSE_(IN|OUT)PUT was set, too?
                } catch (final FsSyncException ex) {
                    if (sync != options // OK, see contract for BitField.and()!
                            && ex.getCause() instanceof FsResourceOpenException)
                        throw NeedsLockRetryException.get(getModel());
                    throw ex;
                }
                return null;
            }
        } // Sync

        writeLocked(new Sync());
    }

    void close(final Closeable closeable) throws IOException {
        final class Close implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                closeable.close();
                return null;
            }
        } // Close

        writeLocked(new Close());
    }

    private interface IOOperation<T> {
        @Nullable T call() throws IOException;
    } // IOOperation

    private final class LockInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            close(in);
        }
    } // LockInputStream

    private final class LockOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            close(out);
        }
    } // LockOutputStream

    private final class LockSeekableChannel
    extends DecoratingSeekableChannel {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            close(channel);
        }
    } // LockSeekableChannel

    @NotThreadSafe
    private static final class ThreadUtil {
        boolean locking;
        final Random rnd;

        ThreadUtil(Random rnd) { this.rnd = rnd; }

        /**
         * Delays the current thread for a random time interval between one and
         * {@link #WAIT_TIMEOUT_MILLIS} milliseconds inclusively.
         * Interrupting the current thread has no effect on this method.
         */
        void pause() {
            Threads.pause(1 + rnd.nextInt(WAIT_TIMEOUT_MILLIS));
        }
    } // ThreadUtil
}