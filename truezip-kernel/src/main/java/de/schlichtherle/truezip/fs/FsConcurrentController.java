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
import net.jcip.annotations.ThreadSafe;

/**
 * A concurrent file system controller which decorates another file system
 * controller in order to provide read/write lock features for multi-threaded
 * access by its clients.
 * 
 * @see     FsConcurrentModel
 * @see     FsNotWriteLockedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsConcurrentController
extends FsDecoratingConcurrentModelController<
        FsController<? extends FsConcurrentModel>> {

    private static final ConcurrentSocketFactory
            CONCURRENT_SOCKET_FACTORY = JSE7.AVAILABLE
                ? ConcurrentSocketFactory.NIO2
                : ConcurrentSocketFactory.OIO;

    private volatile @CheckForNull ReadLock readLock;
    private volatile @CheckForNull WriteLock writeLock;

    /**
     * Constructs a new concurrent file system controller.
     *
     * @param controller the decorated concurrent file system controller.
     */
    public FsConcurrentController(
            FsController<? extends FsConcurrentModel> controller) {
        super(controller);
    }

    @Override
    protected ReadLock readLock() {
        final ReadLock readLock = this.readLock;
        return null != readLock
                ? readLock
                : (this.readLock = getModel().readLock());
    }

    @Override
    protected WriteLock writeLock() {
        final WriteLock writeLock = this.writeLock;
        return null != writeLock
                ? writeLock
                : (this.writeLock = getModel().writeLock());
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getOpenIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getClosedIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.getEntry(name);
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getEntry(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.isReadable(name);
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isReadable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.isWritable(name);
            } finally {
                readLock().unlock();
            }
        } catch (FsNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isWritable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.setReadOnly(name);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return delegate.setTime(name, times, options);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return delegate.setTime(name, types, value, options);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return CONCURRENT_SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return CONCURRENT_SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    @Override
    public void mknod(
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.unlink(name, options);
            final FsController<?> parent;
            if (name.isRoot() && null != (parent = getParent())) {
                // We have just removed the virtual root directory of a
                // federated file system, i.e. archive file.
                // Now unlink the entry for the archive file in the parent file
                // system.
                // Note that this code belongs conceptually in
                // FsFederatedController.
                // However, then this operation wouldn't be atomic anymore.
                parent.unlink(
                        getMountPoint().getPath().resolve(name).getEntryName(),
                        options);
            }
        } finally {
            writeLock().unlock();
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
    private enum ConcurrentSocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsConcurrentController controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsConcurrentController controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsConcurrentController controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsConcurrentController controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsConcurrentController controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsConcurrentController controller,
                OutputSocket <?> output);
    } // ConcurrentSocketFactory

    private final class Nio2Input
    extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return new ConcurrentSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
            } finally {
                writeLock().unlock();
            }
        }
    } // Nio2Input

    private class Input
    extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                readLock().lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (FsNotWriteLockedException ex) {
                assertNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    writeLock().unlock();
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
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return new ConcurrentReadOnlyFile(getBoundSocket().newReadOnlyFile());
            } finally {
                writeLock().unlock();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return new ConcurrentInputStream(getBoundSocket().newInputStream());
            } finally {
                writeLock().unlock();
            }
        }
    } // Input

    private final class Nio2Output
    extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return new ConcurrentSeekableByteChannel(getBoundSocket().newSeekableByteChannel());
            } finally {
                writeLock().unlock();
            }
        }
    } // Nio2Output

    private class Output
    extends DecoratingOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                writeLock().unlock();
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return new ConcurrentOutputStream(getBoundSocket().newOutputStream());
            } finally {
                writeLock().unlock();
            }
        }
    } // Output

    private final class ConcurrentReadOnlyFile
    extends DecoratingReadOnlyFile {
        ConcurrentReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                delegate.close();
            } finally {
                writeLock().unlock();
            }
        }
    } // ConcurrentReadOnlyFile

    private final class ConcurrentSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        ConcurrentSeekableByteChannel(SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                delegate.close();
            } finally {
                writeLock().unlock();
            }
        }
    } // ConcurrentSeekableByteChannel

    private final class ConcurrentInputStream
    extends DecoratingInputStream {
        ConcurrentInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                delegate.close();
            } finally {
                writeLock().unlock();
            }
        }
    } // ConcurrentInputStream

    private final class ConcurrentOutputStream
    extends DecoratingOutputStream {
        ConcurrentOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                delegate.close();
            } finally {
                writeLock().unlock();
            }
        }
    } // ConcurrentInputStream
}
