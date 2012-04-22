/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.LockManagement.isLocking;
import static de.schlichtherle.truezip.kernel.LockManagement.locked;
import static de.truezip.kernel.FsSyncOption.WAIT_CLOSE_IO;
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
 * 
 * @see    LockModel
 * @see    NeedsWriteLockException
 * @see    LockManagement
 * @author Christian Schlichtherle
 */
@Immutable
final class LockController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

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

    <T, X extends Exception> T readOrWriteLocked(Operation<T, X> operation)
    throws X {
        try {
            return readLocked(operation);
        } catch (NeedsWriteLockException ex) {
            return writeLocked(operation);
        }
    }

    <T, X extends Exception> T readLocked(Operation<T, X> operation) throws X {
        return locked(operation, readLock());
    }

    <T, X extends Exception> T writeLocked(Operation<T, X> operation) throws X {
        assert !getModel().isReadLockedByCurrentThread()
                : "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!";
        return locked(operation, writeLock());
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
    public FsEntry entry(final FsEntryName name) throws IOException {
        final class GetEntry implements IOOperation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return controller.entry(name);
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
    public InputSocket<?> input(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.input(name, options));
            }

            @Override
            public Entry localTarget() throws IOException {
                return writeLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().localTarget();
                }
            } // GetLocalTarget

            @Override
            public InputStream stream() throws IOException {
                return writeLocked(new NewStream());
            }

            final class NewStream implements IOOperation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockInputStream(getBoundSocket().stream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return writeLocked(new NewChannel());
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
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(name, options, template));
            }

            @Override
            public Entry localTarget() throws IOException {
                return writeLocked(new GetLocalTarget());
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().localTarget();
                }
            } // GetLocalTarget

            @Override
            public OutputStream stream() throws IOException {
                return writeLocked(new NewStream());
            }

            final class NewStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockOutputStream(getBoundSocket().stream());
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return writeLocked(new NewChannel());
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
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        final boolean locking = isLocking(); // do NOT initialize within Sync!
        final BitField<FsSyncOption> sync = locking
                ? options.and(NOT_WAIT_CLOSE_IO) // may be == options!
                : options;

        final class Sync implements Operation<Void, FsSyncException> {
            @Override
            public Void call() throws FsSyncWarningException, FsSyncException {
                // Prevent potential dead locks by performing a timed wait for
                // open I/O resources if the current thread is already holding
                // a file system lock.
                // Note that a sync in a parent file system is a rare event
                // so that this should not create performance problems, even
                // when accessing deeply nested archive files, e.g. for the
                // integration tests.
                try {
                    controller.sync(sync);
                } catch (final FsSyncWarningException ex) {
                    throw ex; // may be FORCE_CLOSE_(IN|OUT)PUT was set, too?
                } catch (final FsSyncException ex) {
                    if (sync != options) { // OK, see contract for BitField.and()!
                        assert locking;
                        if (ex.getCause() instanceof FsResourceOpenException)
                            throw NeedsLockRetryException.get();
                    }
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
