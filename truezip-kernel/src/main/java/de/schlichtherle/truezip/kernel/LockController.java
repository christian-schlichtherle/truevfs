/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.LockingStrategy.FAST_LOCK;
import static de.schlichtherle.truezip.kernel.LockingStrategy.TIMED_LOCK;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides read/write locking for multi-threaded access by its clients.
 * <p>
 * This controller is a barrier for {@link NeedsWriteLockException}s:
 * Whenever the decorated controller chain throws a
 * {@code NeedsWriteLockException},
 * the read lock gets released before the write lock gets acquired and the
 * operation gets retried.
 * <p>
 * This controller is also an emitter and a barrier for
 * {@link NeedsLockRetryException}s:
 * If a lock can't get immediately acquired, then a
 * {@code NeedsLockRetryException} gets thrown.
 * This will unwind the stack of federated file systems until the 
 * {@code LockController} for the first visited file system is found.
 * This controller will then pause the current thread for a small random amount
 * of milliseconds before retrying the operation.
 * 
 * @see    LockModel
 * @see    LockManagement
 * @see    NeedsWriteLockException
 * @see    NeedsLockRetryException
 * @author Christian Schlichtherle
 */
@Immutable
final class LockController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private final ReadLock readLock;
    private final WriteLock writeLock;

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

    <T, X extends Exception> T fastWriteLocked(Operation<T, X> operation)
    throws X {
        assert !isReadLockedByCurrentThread()
                : "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!";
        return FAST_LOCK.apply(writeLock(), operation);
    }

    <T, X extends Exception> T timedReadOrWriteLocked(Operation<T, X> operation)
    throws X {
        try {
            return timedReadLocked(operation);
        } catch (NeedsWriteLockException discard) {
            return timedWriteLocked(operation);
        }
    }

    <T, X extends Exception> T timedReadLocked(Operation<T, X> operation)
    throws X {
        return TIMED_LOCK.apply(readLock(), operation);
    }

    <T, X extends Exception> T timedWriteLocked(Operation<T, X> operation)
    throws X {
        assert !isReadLockedByCurrentThread()
                : "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!";
        return TIMED_LOCK.apply(writeLock(), operation);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return timedReadOrWriteLocked(new IsReadOnly());
    }

    private final class IsReadOnly implements IOOperation<Boolean> {
        @Override
        public Boolean call() throws IOException {
            return controller.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public FsEntry stat(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        final class Stat implements IOOperation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return controller.stat(options, name);
            }
        } // Stat

        return timedReadOrWriteLocked(new Stat());
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types)
    throws IOException {
        final class CheckAccess implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.checkAccess(options, name, types);
                return null;
            }
        } // CheckAccess

        timedReadOrWriteLocked(new CheckAccess());
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

        timedWriteLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options, final FsEntryName name, final Map<Access, Long> times)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.setTime(options, name, times);
            }
        } // SetTime

        return timedWriteLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types, final long value)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return controller.setTime(options, name, types, value);
            }
        } // SetTime

        return timedWriteLocked(new SetTime());
    }

    @Override
    public InputSocket<?> input(
            final BitField<FsAccessOption> options, final FsEntryName name) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.input(options, name));
            }

            @Override
            public Entry localTarget() throws IOException {
                return fastWriteLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().localTarget();
                }
            } // GetLocalTarget

            @Override
            public InputStream stream() throws IOException {
                return timedWriteLocked(new NewStream());
            }

            final class NewStream implements IOOperation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockInputStream(getBoundSocket().stream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return timedWriteLocked(new NewChannel());
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableChannel(getBoundSocket().channel());
                }
            } // NewChannel
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive
    public OutputSocket<?> output(
            final BitField<FsAccessOption> options, final FsEntryName name, @CheckForNull
    final Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(options, name, template));
            }

            @Override
            public Entry localTarget() throws IOException {
                return fastWriteLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().localTarget();
                }
            } // GetLocalTarget

            @Override
            public OutputStream stream() throws IOException {
                return timedWriteLocked(new NewStream());
            }

            final class NewStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockOutputStream(getBoundSocket().stream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return timedWriteLocked(new NewChannel());
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockSeekableChannel(getBoundSocket().channel());
                }
            } // NewChannel
        } // Output

        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void
    mknod(  final BitField<FsAccessOption> options, final FsEntryName name, final Type type, final Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.mknod(options, name, type, template);
                return null;
            }
        } // Mknod

        timedWriteLocked(new Mknod());
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                controller.unlink(options, name);
                return null;
            }
        } // Unlink

        timedWriteLocked(new Unlink());
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        final class Sync implements Operation<Void, FsSyncException> {
            @Override
            public Void call() throws FsSyncWarningException, FsSyncException {
                controller.sync(options);
                return null;
            }
        } // Sync

        timedWriteLocked(new Sync());
    }

    void close(final Closeable closeable) throws IOException {
        final class Close implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                closeable.close();
                return null;
            }
        } // Close

        timedWriteLocked(new Close());
    }

    private final class LockInputStream
    extends DecoratingInputStream {
        LockInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(in);
        }
    } // LockInputStream

    private final class LockOutputStream
    extends DecoratingOutputStream {
        LockOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(out);
        }
    } // LockOutputStream

    private final class LockSeekableChannel
    extends DecoratingSeekableChannel {
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(channel);
        }
    } // LockSeekableChannel
}
