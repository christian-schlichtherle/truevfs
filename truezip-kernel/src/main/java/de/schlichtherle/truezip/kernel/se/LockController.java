/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.se;

import de.schlichtherle.truezip.kernel.NeedsWriteLockException;
import static de.schlichtherle.truezip.kernel.se.LockingStrategy.FAST_LOCK;
import de.schlichtherle.truezip.kernel.se.LockingStrategy.Operation;
import static de.schlichtherle.truezip.kernel.se.LockingStrategy.TIMED_LOCK;
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

    @Override
    public FsEntry stat(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        class Stat implements IOOperation<FsEntry> {
            @Override
            public FsEntry apply() throws IOException {
                return controller.stat(options, name);
            }
        }
        return timedReadOrWriteLocked(new Stat());
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types)
    throws IOException {
        class CheckAccess implements IOOperation<Void> {
            @Override
            public Void apply() throws IOException {
                controller.checkAccess(options, name, types);
                return null;
            }
        }
        timedReadOrWriteLocked(new CheckAccess());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        class SetReadOnly implements IOOperation<Void> {
            @Override
            public Void apply() throws IOException {
                controller.setReadOnly(name);
                return null;
            }
        }
        timedWriteLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean apply() throws IOException {
                return controller.setTime(options, name, times);
            }
        }
        return timedWriteLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws IOException {
        class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean apply() throws IOException {
                return controller.setTime(options, name, types, value);
            }
        }
        return timedWriteLocked(new SetTime());
    }

    @Override
    public InputSocket<?> input(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        @NotThreadSafe
        class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.input(options, name));
            }

            @Override
            public Entry localTarget() throws IOException {
                class GetLocalTarget implements IOOperation<Entry> {
                    @Override
                    public Entry apply() throws IOException {
                        return boundSocket().localTarget();
                    }
                }
                return fastWriteLocked(new GetLocalTarget());
            }

            @Override
            public InputStream stream() throws IOException {
                class NewStream implements IOOperation<InputStream> {
                    @Override
                    public InputStream apply() throws IOException {
                        return new LockInputStream(boundSocket().stream());
                    }
                }
                return timedWriteLocked(new NewStream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                class NewChannel implements IOOperation<SeekableByteChannel> {
                    @Override
                    public SeekableByteChannel apply() throws IOException {
                        return new LockSeekableChannel(boundSocket().channel());
                    }
                }
                return timedWriteLocked(new NewChannel());
            }
        }
        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive
    public OutputSocket<?> output(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(options, name, template));
            }

            @Override
            public Entry localTarget() throws IOException {
                class GetLocalTarget implements IOOperation<Entry> {
                    @Override
                    public Entry apply() throws IOException {
                        return boundSocket().localTarget();
                    }
                }
                return fastWriteLocked(new GetLocalTarget());
            }

            @Override
            public OutputStream stream() throws IOException {
                class NewStream implements IOOperation<OutputStream> {
                    @Override
                    public OutputStream apply() throws IOException {
                        return new LockOutputStream(boundSocket().stream());
                    }
                }
                return timedWriteLocked(new NewStream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                class NewChannel implements IOOperation<SeekableByteChannel> {
                    @Override
                    public SeekableByteChannel apply() throws IOException {
                        return new LockSeekableChannel(boundSocket().channel());
                    }
                }
                return timedWriteLocked(new NewChannel());
            }
        }
        return new Output();
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
        LockSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(channel);
        }
    } // LockSeekableChannel

    void close(final Closeable closeable) throws IOException {
        class Close implements IOOperation<Void> {
            @Override
            public Void apply() throws IOException {
                closeable.close();
                return null;
            }
        }
        timedWriteLocked(new Close());
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Type type,
            final Entry template)
    throws IOException {
        class Mknod implements IOOperation<Void> {
            @Override
            public Void apply() throws IOException {
                controller.mknod(options, name, type, template);
                return null;
            }
        }
        timedWriteLocked(new Mknod());
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        class Unlink implements IOOperation<Void> {
            @Override
            public Void apply() throws IOException {
                controller.unlink(options, name);
                return null;
            }
        }
        timedWriteLocked(new Unlink());
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        class Sync implements Operation<Void, FsSyncException> {
            @Override
            public Void apply() throws FsSyncWarningException, FsSyncException {
                controller.sync(options);
                return null;
            }
        }
        timedWriteLocked(new Sync());
    }

    @SuppressWarnings("MarkerInterface")
    private interface IOOperation<V> extends Operation<V, IOException> {
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
}
