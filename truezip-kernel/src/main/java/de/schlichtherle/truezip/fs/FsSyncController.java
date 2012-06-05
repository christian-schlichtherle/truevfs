/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.fs.FsSyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.fs.FsSyncOptions.RESET;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
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
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.swing.Icon;

/**
 * Performs a {@link FsController#sync(BitField) sync} operation on the
 * file system if and only if any decorated file system controller throws an
 * {@link FsNeedsSyncException}.
 * 
 * @see    FsNeedsSyncException
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsSyncController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private static final BitField<FsSyncOption> NOT_WAIT_CLOSE_IO
            = BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT).not();

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsSyncController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    void sync(final FsNeedsSyncException trigger)
    throws IOException {
        checkWriteLockedByCurrentThread();
        final FsSyncWarningException fuse
                = new FsSyncWarningException(getModel(), trigger);
        final FsSyncExceptionBuilder ied = new FsSyncExceptionBuilder();
        try {
            ied.warn(fuse);     // charge fuse
            sync(SYNC, ied);    // charge load
            ied.check();        // pull trigger
            throw new AssertionError("Expected an instance of the " + FsSyncException.class);
        } catch (final FsSyncWarningException damage) {
            if (damage != fuse) // check for dud
                throw damage;
        }
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        while (true) {
            try {
                return delegate.getOpenIcon();
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        while (true) {
            try {
                return delegate.getClosedIcon();
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return delegate.isReadOnly();
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return delegate.getEntry(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isReadable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isWritable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isExecutable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        while (true) {
            try {
                delegate.setReadOnly(name);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, times, options);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, types, value, options);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this, name, options);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this, name, options, template);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        while (true) {
            try {
                delegate.mknod(name, type, options, template);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                // HC SVNT DRACONES!
                delegate.unlink(name, options);
                if (name.isRoot()) {
                    // Make the file system controller chain eligible for GC.
                    delegate.sync(RESET);
                }
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        final BitField<FsSyncOption> modified = modify(options);
        try {
            delegate.sync(modified);
        } catch (final FsSyncWarningException ex) {
            throw ex; // may be FORCE_CLOSE_(IN|OUT)PUT was set, too?
        } catch (final FsSyncException ex) {
            if (modified != options)
                if (ex.getCause() instanceof FsResourceOpenException)
                    throw FsNeedsLockRetryException.get(getModel());
            throw ex;
        }
    }

    /**
     * Modify the sync options so that no dead lock can appear due to waiting
     * for I/O resources in a recursive file system operation.
     * 
     * @param  options the sync options
     * @return the potentially modified sync options.
     */
    static BitField<FsSyncOption> modify(final BitField<FsSyncOption> options) {
        final boolean isRecursive = 1 < FsLockController.getLockCount();
        final BitField<FsSyncOption> result = isRecursive
                ? options.and(NOT_WAIT_CLOSE_IO)
                : options;
        assert result == options == result.equals(options) : "Broken contract in BitField.and()!";
        assert result == options || isRecursive;
        return result;
    }

    void close(final Closeable closeable) throws IOException {
        while (true) {
            try {
                closeable.close();
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Nio2Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Nio2Output(name, options, template);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsSyncController controller,
                FsEntryName name,
                BitField<FsInputOption> options);
        
        abstract OutputSocket<?> newOutputSocket(
                FsSyncController controller,
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
            while (true) {
                try {
                    return new SyncSeekableByteChannel(
                            getBoundSocket().newSeekableByteChannel());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Nio2Input

    @Immutable
    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsSyncController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            while (true) {
                try {
                    return new SyncReadOnlyFile(
                            getBoundSocket().newReadOnlyFile());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            while (true) {
                try {
                    return new SyncInputStream(
                            getBoundSocket().newInputStream());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
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
            while (true) {
                try {
                    return new SyncSeekableByteChannel(
                            getBoundSocket().newSeekableByteChannel());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Nio2Output

    @Immutable
    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(FsSyncController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            while (true) {
                try {
                    return new SyncOutputStream(
                            getBoundSocket().newOutputStream());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Output

    private final class SyncReadOnlyFile
    extends DecoratingReadOnlyFile {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        SyncReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // SyncReadOnlyFile

    private final class SyncSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        SyncSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // SyncSeekableByteChannel

    private final class SyncInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        SyncInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // SyncInputStream

    private final class SyncOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        SyncOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            close(delegate);
        }
    } // SyncOutputStream
}