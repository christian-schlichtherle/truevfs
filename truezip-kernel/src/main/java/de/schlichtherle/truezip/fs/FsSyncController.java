/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.ProxyInputSocket;
import de.schlichtherle.truezip.socket.ProxyOutputSocket;
import de.schlichtherle.truezip.util.BitField;
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
 * @param  <M> the type of the file system model.
 * @see    FsNeedsSyncException
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsSyncController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsSyncController(FsController<? extends M> controller) {
        super(controller);
    }

    private void sync() throws IOException {
        delegate.sync(SYNC);
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        while (true) {
            try {
                return delegate.getOpenIcon();
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        while (true) {
            try {
                return delegate.getClosedIcon();
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return delegate.isReadOnly();
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return delegate.getEntry(name);
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isReadable(name);
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isWritable(name);
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isExecutable(name);
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        while (true) {
            try {
                delegate.setReadOnly(name);
                return;
            } catch (FsNeedsSyncException discard) {
                sync();
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
            } catch (FsNeedsSyncException discard) {
                sync();
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
            } catch (FsNeedsSyncException discard) {
                sync();
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
            } catch (FsNeedsSyncException discard) {
                sync();
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
                delegate.unlink(name, options);
                return;
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    void close(final Closeable closeable) throws IOException {
        while (true) {
            try {
                closeable.close();
                return;
            } catch (FsNeedsSyncException discard) {
                sync();
            }
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?> controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Nio2Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?> controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Nio2Output(name, options, template);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?> controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?> controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsSyncController<?> controller,
                FsEntryName name,
                BitField<FsInputOption> options);
        
        abstract OutputSocket<?> newOutputSocket(
                FsSyncController<?> controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                @CheckForNull Entry template);
    } // SocketFactory

    private final class Nio2Input extends Input {
        Nio2Input(  final FsEntryName name,
                    final BitField<FsInputOption> options) {
            super(name, options);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            while (true) {
                try {
                    return new SyncSeekableByteChannel(super.newSeekableByteChannel());
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }
    } // Nio2Input

    private class Input extends ProxyInputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            this.name = name;
            this.options = options;
        }

        @Override
        protected final InputSocket<?> getProxiedDelegate()
        throws IOException {
            return FsSyncController.this.delegate
                    .getInputSocket(name, options);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return super.getLocalTarget();
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            while (true) {
                try {
                    return new SyncReadOnlyFile(super.newReadOnlyFile());
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            while (true) {
                try {
                    return new SyncInputStream(super.newInputStream());
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }
    } // Input

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
                    return new SyncSeekableByteChannel(super.newSeekableByteChannel());
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }
    } // Nio2Output

    private class Output extends ProxyOutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final @CheckForNull Entry template;

        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        protected final OutputSocket<?> getProxiedDelegate()
        throws IOException {
            return FsSyncController.this.delegate
                    .getOutputSocket(name, options, template);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return super.getLocalTarget();
                } catch (FsNeedsSyncException discard) {
                    sync();
                }
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            while (true) {
                try {
                    return new SyncOutputStream(super.newOutputStream());
                } catch (FsNeedsSyncException discard) {
                    sync();
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
            FsSyncController.this.close(delegate);
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
            FsSyncController.this.close(delegate);
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
            FsSyncController.this.close(delegate);
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
            FsSyncController.this.close(delegate);
        }
    } // SyncOutputStream
}
