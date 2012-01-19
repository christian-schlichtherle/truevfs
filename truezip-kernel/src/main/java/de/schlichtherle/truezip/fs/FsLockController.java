/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.swing.Icon;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * A file system controller which decorates another file system controller
 * in order to provide read/write locking for multi-threaded access by its
 * clients.
 * 
 * @see     FsLockModel
 * @see     FsNeedsWriteLockException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsLockController
extends FsLockModelDecoratingController<
        FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;
    private static final ThreadLocal<ThreadData> threadData = (JSE7.AVAILABLE
            ? ThreadLocalDataFactory.NEW
            : ThreadLocalDataFactory.OLD
                ).newThreadLocalData();

    // These fields don't need to be volatile because reads and writes of
    // references are always atomic.
    // See The Java Language Specification, Third Edition, section 17.7
    // "Non-atomic Treatment of double and long".
    private /*volatile*/ @CheckForNull ReadLock readLock;
    private /*volatile*/ @CheckForNull WriteLock writeLock;

    /**
     * Constructs a new file system lock controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsLockController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    @Override
    protected ReadLock readLock() {
        final ReadLock lock = this.readLock;
        return null != lock ? lock : (this.readLock = getModel().readLock());
    }

    @Override
    protected WriteLock writeLock() {
        final WriteLock lock = this.writeLock;
        return null != lock ? lock : (this.writeLock = getModel().writeLock());
    }

    <T> T callReadOrWriteLocked(IOOperation<T> operation)
    throws IOException {
        try {
            return callReadLocked(operation);
        } catch (FsNeedsWriteLockException ex) {
            return callWriteLocked(operation);
        }
    }

    <T> T callReadLocked(IOOperation<T> operation) throws IOException {
        return callLocked(operation, readLock(), IOExceptionHandler.INSTANCE);
    }

    <T> T callWriteLocked(IOOperation<T> operation) throws IOException {
        checkNotReadLockedByCurrentThread();
        return callLocked(operation, writeLock(), IOExceptionHandler.INSTANCE);
    }

    /**
     * Tries to call the given consistent operation while holding the given
     * lock.
     * <p>
     * If this is the first execution of this method on the call stack of the
     * current thread, then the lock gets acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason and the thrown exception chain contains an
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
     * @param  <X> The exception type of the operation.
     * @param  operation The atomic operation.
     * @param  lock The lock to hold while calling the operation.
     * @param  handler The exception handler to deal with an
     *         {@link FsNeedsLockRetryException} which may result from a
     *         failing {@link Lock#tryLock()}.
     * @return The result of the operation.
     * @throws X As thrown by the operation.
     */
    @SuppressWarnings("unchecked")
    private static <T, X extends Exception> T
    callLocked( final Operation<T, X> operation,
                final Lock lock,
                final ExceptionHandler<? super FsNeedsLockRetryException, X> handler)
    throws X {
        final ThreadData thread = threadData.get();
        if (thread.locking) {
            if (!lock.tryLock())
                throw handler.fail(new FsNeedsLockRetryException());
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
                } catch (Exception ex) {
                    if (!needsLockRetry(ex))
                        throw (X) ex;
                    thread.pause();
                }
            }
        }
    }

    private static boolean needsLockRetry(final Throwable t) {
        return t instanceof FsNeedsLockRetryException
                || null != t && needsLockRetry(t.getCause());
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        class GetOpenIcon implements IOOperation<Icon> {
            @Override
            public Icon call() throws IOException {
                return delegate.getOpenIcon();
            }
        } // GetOpenIcon

        return callReadOrWriteLocked(new GetOpenIcon());
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        class GetClosedIcon implements IOOperation<Icon> {
            @Override
            public Icon call() throws IOException {
                return delegate.getClosedIcon();
            }
        } // GetClosedIcon

        return callReadOrWriteLocked(new GetClosedIcon());
    }

    @Override
    public boolean isReadOnly() throws IOException {
        class IsReadOnly implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isReadOnly();
            }
        } // IsReadOnly

        return callReadOrWriteLocked(new IsReadOnly());
    }

    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        class GetEntry implements IOOperation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return delegate.getEntry(name);
            }
        } // GetEntry

        return callReadOrWriteLocked(new GetEntry());
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        class IsReadable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isReadable(name);
            }
        } // IsReadable

        return callReadOrWriteLocked(new IsReadable());
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        class IsWritable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isWritable(name);
            }
        } // IsWritable

        return callReadOrWriteLocked(new IsWritable());
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        class IsExecutable implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isExecutable(name);
            }
        } // IsExecutable

        return callReadOrWriteLocked(new IsExecutable());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        class SetReadOnly implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.setReadOnly(name);
                return null;
            }
        } // SetReadOnly

        callWriteLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, times, options);
            }
        } // class SetTime

        return callWriteLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, types, value, options);
            }
        } // class SetTime

        return callWriteLocked(new SetTime());
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    @Override
    public void mknod(
            final @NonNull FsEntryName name,
            final @NonNull Type type,
            final @NonNull BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        class Mknod implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.mknod(name, type, options, template);
                return null;
            }
        } // Mknod

        callWriteLocked(new Mknod());
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        class Unlink implements IOOperation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.unlink(name, options);
                return null;
            }
        } // Unlink

        callWriteLocked(new Unlink());
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class Sync implements Operation<Void, X> {
            @Override
            public Void call() throws X {
                delegate.sync(options, handler);
                return null;
            }
        } // Sync

        class Handler implements ExceptionHandler<FsNeedsLockRetryException, X> {
            @Override
            public X fail(FsNeedsLockRetryException cause) {
                return handler.fail(new FsSyncException(getModel(), cause));
            }

            @Override
            public void warn(FsNeedsLockRetryException cause) throws X {
                handler.warn(new FsSyncException(getModel(), cause));
            }
        } // Handler

        callLocked(new Sync(), writeLock(), new Handler());
    }

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsLockController controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsLockController controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsLockController controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsLockController controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsLockController controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsLockController controller,
                OutputSocket <?> output);
    } // SocketFactory

    @NotThreadSafe
    private final class Nio2Input extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            class NewSeekableByteChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockingSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
                }
            } // NewSeekableByteChannel

            return callWriteLocked(new NewSeekableByteChannel());
        }
    } // Nio2Input

    @NotThreadSafe
    private class Input extends DecoratingInputSocket<Entry> {
        Entry local;

        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return null != local ? local : (local = getLocalTarget0());
        }

        private Entry getLocalTarget0() throws IOException {
            class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget

            return callReadOrWriteLocked(new GetLocalTarget());
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            class NewReadOnlyFile implements IOOperation<ReadOnlyFile> {
                @Override
                public ReadOnlyFile call() throws IOException {
                    return new LockingReadOnlyFile(getBoundSocket().newReadOnlyFile());
                }
            } // NewReadOnlyFile

            return callWriteLocked(new NewReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            class NewInputStream implements IOOperation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockingInputStream(getBoundSocket().newInputStream());
                }
            } // NewInputStream

            return callWriteLocked(new NewInputStream());
        }
    } // Input

    @NotThreadSafe
    private final class Nio2Output extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            class NewSeekableByteChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call() throws IOException {
                    return new LockingSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
                }
            } // NewSeekableByteChannel

            return callWriteLocked(new NewSeekableByteChannel());
        }
    } // Nio2Output

    @NotThreadSafe
    private class Output extends DecoratingOutputSocket<Entry> {
        Entry local;

        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return null != local ? local : (local = getLocalTarget0());
        }

        private Entry getLocalTarget0() throws IOException {
            class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget

            return callReadOrWriteLocked(new GetLocalTarget());
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            class NewOutputStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockingOutputStream(getBoundSocket().newOutputStream());
                }
            } // NewOutputStream

            return callWriteLocked(new NewOutputStream());
        }
    } // Output

    private final class LockingReadOnlyFile
    extends DecoratingReadOnlyFile {
        LockingReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            class Close implements IOOperation<Void> {
                @Override
                public Void call() throws IOException {
                    delegate.close();
                    return null;
                }
            } // Close

            callWriteLocked(new Close());
        }
    } // LockingReadOnlyFile

    private final class LockingSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        LockingSeekableByteChannel(SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            class Close implements IOOperation<Void> {
                @Override
                public Void call() throws IOException {
                    delegate.close();
                    return null;
                }
            } // Close

            callWriteLocked(new Close());
        }
    } // LockingSeekableByteChannel

    private final class LockingInputStream
    extends DecoratingInputStream {
        LockingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            class Close implements IOOperation<Void> {
                @Override
                public Void call() throws IOException {
                    delegate.close();
                    return null;
                }
            } // Close

            callWriteLocked(new Close());
        }
    } // LockingInputStream

    private final class LockingOutputStream
    extends DecoratingOutputStream {
        LockingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            class Close implements IOOperation<Void> {
                @Override
                public Void call() throws IOException {
                    delegate.close();
                    return null;
                }
            } // Close

            callWriteLocked(new Close());
        }
    } // LockingOutputStream

    @NotThreadSafe
    private static final class ThreadData {
        boolean locking;
        final Random rnd;

        ThreadData(Random rnd) { this.rnd = rnd; }

        void pause () {
            boolean interrupted = false;
            try {
                Thread.sleep(rnd.nextInt(WAIT_TIMEOUT));
            } catch (InterruptedException ex) {
                interrupted = true;
            }
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    } // ThreadData

    @Immutable
    private enum ThreadLocalDataFactory {
        OLD {
            @Override
            ThreadLocal<ThreadData> newThreadLocalData() {
                return new ThreadLocal<ThreadData>() {
                    @Override
                    public ThreadData initialValue() {
                        return new ThreadData(new Random());
                    }
                };
            }
        },
        
        NEW {
            @Override
            ThreadLocal<ThreadData> newThreadLocalData() {
                return new ThreadLocal<ThreadData>() {
                    @Override
                    public ThreadData initialValue() {
                        return new ThreadData(ThreadLocalRandom.current());
                    }
                };
            }
        };

        abstract ThreadLocal<ThreadData> newThreadLocalData();
    } // ThreadLocalDataFactory

    private interface Operation<T, X extends Exception> extends Callable<T> {
        @Override T call() throws X;
    } // Operation

    private interface IOOperation<T> extends Operation<T, IOException> {
    } // IOOperation

    @Immutable
    private static final class IOExceptionHandler
    implements ExceptionHandler<IOException, IOException> {
        static final IOExceptionHandler INSTANCE = new IOExceptionHandler();

        @Override
        public IOException fail(IOException cause) {
            return cause;
        }

        @Override
        public void warn(IOException cause) throws IOException {
            throw cause;
        }
    } // IOExceptionHandler
}
