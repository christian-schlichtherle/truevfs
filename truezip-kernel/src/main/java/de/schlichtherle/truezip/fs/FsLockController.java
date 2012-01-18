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

    private static final LockCounter lockCount = new LockCounter();

    private volatile @CheckForNull ReadLock readLock;
    private volatile @CheckForNull WriteLock writeLock;

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

    private void acquireReadLock() throws FsException {
        /*if (lockCount.get() > 0) {
            if (!readLock().tryLock()) {
                throw new FsNeedsLockRetryException();
            }
        } else {
            readLock().lock();
        }
        lockCount.inc();*/
        readLock().lock();
    }

    private void releaseReadLock() {
        //lockCount.dec();
        readLock().unlock();
    }

    private void acquireWriteLock(FsNeedsWriteLockException ex)
    throws FsException {
        assertNotReadLockedByCurrentThread(ex);
        writeLock().lock();
    }

    private void releaseWriteLock() {
        writeLock().unlock();
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.getOpenIcon();
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.getOpenIcon();
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.getClosedIcon();
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.getClosedIcon();
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.isReadOnly();
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.isReadOnly();
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.getEntry(name);
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.getEntry(name);
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.isReadable(name);
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.isReadable(name);
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.isWritable(name);
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.isWritable(name);
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        try {
            acquireReadLock();
            try {
                return delegate.isExecutable(name);
            } finally {
                releaseReadLock();
            }
        } catch (FsNeedsWriteLockException ex) {
            acquireWriteLock(ex);
            try {
                return delegate.isExecutable(name);
            } finally {
                releaseWriteLock();
            }
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        acquireWriteLock(null);
        try {
            delegate.setReadOnly(name);
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        acquireWriteLock(null);
        try {
            return delegate.setTime(name, times, options);
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        acquireWriteLock(null);
        try {
            return delegate.setTime(name, types, value, options);
        } finally {
            releaseWriteLock();
        }
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
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        acquireWriteLock(null);
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        acquireWriteLock(null);
        try {
            delegate.unlink(name, options);
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<FsSyncOption> options,
            @NonNull final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        writeLock().lock();
        try {
            delegate.sync(options, handler);
        } finally {
            writeLock().unlock();
        }
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
            acquireWriteLock(null);
            try {
                return new LockingSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
            } finally {
                releaseWriteLock();
            }
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
            // Caching the local target prevents a dead lock in complex nested
            // archive file copying scenarios.
            // Skip it and run the integration tests concurrently to watch the
            // effect.
            return null != local ? local : (local = getLocalTarget0());
        }

        private Entry getLocalTarget0() throws IOException {
            try {
                acquireReadLock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    releaseReadLock();
                }
            } catch (FsNeedsWriteLockException ex) {
                acquireWriteLock(ex);
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    releaseWriteLock();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            acquireWriteLock(null);
            try {
                return new LockingReadOnlyFile(getBoundSocket().newReadOnlyFile());
            } finally {
                releaseWriteLock();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            acquireWriteLock(null);
            try {
                return new LockingInputStream(getBoundSocket().newInputStream());
            } finally {
                releaseWriteLock();
            }
        }
    } // Input

    @NotThreadSafe
    private final class Nio2Output extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            acquireWriteLock(null);
            try {
                return new LockingSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
            } finally {
                releaseWriteLock();
            }
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
            // Caching the local target prevents a dead lock in complex nested
            // archive file copying scenarios.
            // Skip it and run the integration tests concurrently to watch the
            // effect.
            return null != local ? local : (local = getLocalTarget0());
        }

        private Entry getLocalTarget0() throws IOException {
            acquireWriteLock(null);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                releaseWriteLock();
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            acquireWriteLock(null);
            try {
                return new LockingOutputStream(getBoundSocket().newOutputStream());
            } finally {
                releaseWriteLock();
            }
        }
    } // Output

    private final class LockingReadOnlyFile
    extends DecoratingReadOnlyFile {
        LockingReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            acquireWriteLock(null);
            try {
                delegate.close();
            } finally {
                releaseWriteLock();
            }
        }
    } // LockingReadOnlyFile

    private final class LockingSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        LockingSeekableByteChannel(SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            acquireWriteLock(null);
            try {
                delegate.close();
            } finally {
                releaseWriteLock();
            }
        }
    } // LockingSeekableByteChannel

    private final class LockingInputStream
    extends DecoratingInputStream {
        LockingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            acquireWriteLock(null);
            try {
                delegate.close();
            } finally {
                releaseWriteLock();
            }
        }
    } // LockingInputStream

    private final class LockingOutputStream
    extends DecoratingOutputStream {
        LockingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            acquireWriteLock(null);
            try {
                delegate.close();
            } finally {
                releaseWriteLock();
            }
        }
    } // LockingOutputStream

    private static final class LockCounter extends ThreadLocal<Integer> {
        @Override
        public Integer initialValue() {
            return 0;
        }

        void inc() {
            set(get() + 1);
        }

        void dec() {
            set(get() - 1);
        }
    } // ThreadLocalInteger
}
