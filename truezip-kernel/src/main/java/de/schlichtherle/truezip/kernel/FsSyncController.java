/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.FsEntry;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.addr.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.sbc.DecoratingSeekableByteChannel;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Performs a {@link FsController#sync(BitField) sync} operation on the
 * file system if and only if any decorated file system controller throws an
 * {@link FsNeedsSyncException}.
 * 
 * @see    FsNeedsSyncException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsSyncController
extends FsSyncDecoratingController<FsModel, FsController<?>> {

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    FsSyncController(FsController<?> controller) {
        super(controller);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return controller.isReadOnly();
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
                return controller.getEntry(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isReadable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isWritable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isExecutable(name);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        while (true) {
            try {
                controller.setReadOnly(name);
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
            final BitField<AccessOption> options)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(name, times, options);
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
            final BitField<AccessOption> options)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(name, types, value, options);
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<AccessOption> options) {
        return new Input(name, options);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<AccessOption> options,
                                            @CheckForNull Entry template) {
        return new Output(name, options, template);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<AccessOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        while (true) {
            try {
                controller.mknod(name, type, options, template);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<AccessOption> options)
    throws IOException {
        while (true) {
            try {
                controller.unlink(name, options);
                return;
            } catch (FsNeedsSyncException ex) {
                sync(ex);
            }
        }
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

    @NotThreadSafe
    private final class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<AccessOption> options) {
            super(controller.getInputSocket(name, options));
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
        public SeekableByteChannel newChannel() throws IOException {
            while (true) {
                try {
                    return new SyncSeekableByteChannel(
                            getBoundSocket().newChannel());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public InputStream newStream() throws IOException {
            while (true) {
                try {
                    return new SyncInputStream(
                            getBoundSocket().newStream());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }
    } // Input

    @NotThreadSafe
    private final class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<AccessOption> options,
                final @CheckForNull Entry template) {
            super(controller.getOutputSocket(name, options, template));
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
        public SeekableByteChannel newChannel() throws IOException {
            while (true) {
                try {
                    return new SyncSeekableByteChannel(
                            getBoundSocket().newChannel());
                } catch (FsNeedsSyncException ex) {
                    sync(ex);
                }
            }
        }

        @Override
        public OutputStream newStream() throws IOException {
            while (true) {
                try {
                    return new SyncOutputStream(
                            getBoundSocket().newStream());
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
            close(rof);
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
            close(sbc);
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
            close(in);
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
            close(out);
        }
    } // SyncOutputStream
}