/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.addr.FsEntryName;
import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.cio.Entry.Access;
import de.schlichtherle.truezip.cio.Entry.Type;
import de.schlichtherle.truezip.fs.option.FsInputOption;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import de.schlichtherle.truezip.fs.option.FsSyncOption;
import static de.schlichtherle.truezip.fs.option.FsSyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.option.FsSyncOption.WAIT_CLOSE_OUTPUT;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.cio.DecoratingInputSocket;
import de.schlichtherle.truezip.cio.DecoratingOutputSocket;
import de.schlichtherle.truezip.cio.InputSocket;
import de.schlichtherle.truezip.cio.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import de.schlichtherle.truezip.util.Threads;
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
 * @see    FsLockModel
 * @see    FsNeedsWriteLockException
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@Immutable
final class FsLockController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private static final ThreadLocal<ThreadUtil> threadUtil = (JSE7.AVAILABLE
            ? ThreadLocalUtilFactory.NEW
            : ThreadLocalUtilFactory.OLD
                ).newThreadLocalUtil();

    private static final BitField<FsSyncOption> NOT_WAIT_CLOSE_IO
            = BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT).not();

    private final ReadLock readLock;
    private final WriteLock writeLock;

    /**
     * Constructs a new file system lock controller.
     *
     * @param controller the decorated file system controller.
     */
    FsLockController(FsController<? extends FsLockModel> controller) {
        super(controller);
        this.readLock = getModel().readLock();
        this.writeLock = getModel().writeLock();
    }

    @Override
    protected ReadLock readLock() {
        return this.readLock;
    }

    @Override
    protected WriteLock writeLock() {
        return this.writeLock;
    }

    <T> T readOrWriteLocked(IOOperation<T> operation)
    throws IOException {
        try {
            return readLocked(operation);
        } catch (FsNeedsWriteLockException ex) {
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
     * {@link FsNeedsLockRetryException}, then the lock gets temporarily
     * released and the current thread gets paused for a small random time
     * interval before this procedure starts over again.
     * Otherwise, the exception chain gets just passed on to the caller.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call
     * stack of the current thread, then the lock gets acquired using
     * {@link Lock#tryLock()} instead.
     * If this fails, an {@code FsNeedsLockRetryException} gets created and
     * passed to the given exception handler for mapping before finally
     * throwing the resulting exception by executing
     * {@code throw handler.fail(new FsNeedsLockRetryException())}.
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
     * @throws FsNeedsLockRetryException See above.
     */
    private <T> T locked(final IOOperation<T> operation, final Lock lock)
    throws IOException {
        final ThreadUtil thread = threadUtil.get();
        if (thread.locking) {
            if (!lock.tryLock())
                throw FsNeedsLockRetryException.get(getModel());
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
                } catch (final IOException ex) {
                    if (!(ex instanceof FsNeedsLockRetryException))
                        throw ex;
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
            return delegate.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        final class GetEntry implements IOOperation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return delegate.getEntry(name);
            }
        } // GetEntry

        return readOrWriteLocked(new GetEntry());
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final class IsReadable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isReadable(name);
            }
        } // IsReadable

        return readOrWriteLocked(new IsReadable());
    }
    
    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final class IsWritable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isWritable(name);
            }
        } // IsWritable

        return readOrWriteLocked(new IsWritable());
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final class IsExecutable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isExecutable(name);
            }
        } // IsExecutable

        return readOrWriteLocked(new IsExecutable());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final class SetReadOnly implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.setReadOnly(name);
                return null;
            }
        } // SetReadOnly

        writeLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, times, options);
            }
        } // class SetTime

        return writeLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, types, value, options);
            }
        } // class SetTime

        return writeLocked(new SetTime());
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this, name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this, name, options, template);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void
    mknod(  final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.mknod(name, type, options, template);
                return null;
            }
        } // Mknod

        writeLocked(new Mknod());
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.unlink(name, options);
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
                    delegate.sync(sync, handler);
                } catch (final FsSyncWarningException ex) {
                    throw ex; // may be FORCE_CLOSE_(IN|OUT)PUT was set, too?
                } catch (final FsSyncException ex) {
                    if (sync != options // OK, see contract for BitField.and()!
                            && ex.getCause() instanceof FsResourceOpenException)
                        throw FsNeedsLockRetryException.get(getModel());
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

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Nio2Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Nio2Output(name, options, template);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsLockController controller,
                FsEntryName name,
                BitField<FsInputOption> options);
        
        abstract OutputSocket<?> newOutputSocket(
                FsLockController controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                @CheckForNull Entry template);
    } // SocketFactory

    @Immutable
    private final class Nio2Input extends Input {
        Nio2Input(  final FsEntryName name,
                    final BitField<FsInputOption> options) {
            super(name, options);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            class NewSeekableByteChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableByteChannel(
                            getBoundDelegate().newSeekableByteChannel());
                }
            } // NewSeekableByteChannel

            return writeLocked(new NewSeekableByteChannel());
        }
    } // Nio2Input

    @Immutable
    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsLockController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundDelegate().getLocalTarget();
                }
            } // GetLocalTarget

            return writeLocked(new GetLocalTarget());
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            class NewReadOnlyFile implements IOOperation<ReadOnlyFile> {
                @Override
                public ReadOnlyFile call() throws IOException {
                    return new LockReadOnlyFile(
                            getBoundDelegate().newReadOnlyFile());
                }
            } // NewReadOnlyFile

            return writeLocked(new NewReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            class NewInputStream implements IOOperation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockInputStream(
                            getBoundDelegate().newInputStream());
                }
            } // NewInputStream

            return writeLocked(new NewInputStream());
        }
    } // Input

    @Immutable
    private final class Nio2Output extends Output {
        Nio2Output( final FsEntryName name,
                    final BitField<FsOutputOption> options,
                    final @CheckForNull Entry template) {
            super(name, options, template);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            class NewSeekableByteChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableByteChannel(
                            getBoundDelegate().newSeekableByteChannel());
                }
            } // NewSeekableByteChannel

            return writeLocked(new NewSeekableByteChannel());
        }
    } // Nio2Output

    @Immutable
    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(FsLockController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundDelegate().getLocalTarget();
                }
            } // GetLocalTarget

            return writeLocked(new GetLocalTarget());
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            class NewOutputStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockOutputStream(
                            getBoundDelegate().newOutputStream());
                }
            } // NewOutputStream

            return writeLocked(new NewOutputStream());
        }
    } // Output

    private final class LockReadOnlyFile
    extends DecoratingReadOnlyFile {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // LockReadOnlyFile

    private final class LockSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // LockSeekableByteChannel

    private final class LockInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        LockInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
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
            close(delegate);
        }
    } // LockOutputStream

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

    @Immutable
    private enum ThreadLocalUtilFactory {
        NEW {
            @Override
            ThreadLocal<ThreadUtil> newThreadLocalUtil() {
                return new ThreadLocal<ThreadUtil>() {
                    @Override
                    public ThreadUtil initialValue() {
                        return new ThreadUtil(ThreadLocalRandom.current());
                    }
                };
            }
        },

        OLD {
            @Override
            ThreadLocal<ThreadUtil> newThreadLocalUtil() {
                return new ThreadLocal<ThreadUtil>() {
                    @Override
                    public ThreadUtil initialValue() {
                        return new ThreadUtil(new Random());
                    }
                };
            }
        };

        abstract ThreadLocal<ThreadUtil> newThreadLocalUtil();
    } // ThreadLocalToolFactory
}
