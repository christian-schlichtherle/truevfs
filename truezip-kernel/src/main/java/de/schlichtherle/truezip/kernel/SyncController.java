/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.FsEntry;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
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
 * {@link NeedsSyncException}.
 * 
 * @see    NeedsSyncException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SyncController
extends SyncDecoratingController<FsModel, FsController<?>> {

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    SyncController(FsController<?> controller) {
        super(controller);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return controller.isReadOnly();
            } catch (NeedsSyncException ex) {
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
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isReadable(name);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isWritable(name);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return controller.isExecutable(name);
            } catch (NeedsSyncException ex) {
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
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsAccessOption> options)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(name, times, options);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsAccessOption> options)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(name, types, value, options);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.getInputSocket(name, options));
            }

            @Override
            public Entry localTarget() throws IOException {
                while (true) {
                    try {
                        return getBoundSocket().localTarget();
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public InputStream stream() throws IOException {
                while (true) {
                    try {
                        return new SyncInputStream(
                                getBoundSocket().stream());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                while (true) {
                    try {
                        return new SyncSeekableChannel(
                                getBoundSocket().channel());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.getOutputSocket(name, options, template));
            }

            @Override
            public Entry localTarget() throws IOException {
                while (true) {
                    try {
                        return getBoundSocket().localTarget();
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                while (true) {
                    try {
                        return new SyncSeekableChannel(
                                getBoundSocket().channel());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public OutputStream stream() throws IOException {
                while (true) {
                    try {
                        return new SyncOutputStream(
                                getBoundSocket().stream());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        while (true) {
            try {
                controller.mknod(name, type, options, template);
                return;
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsAccessOption> options)
    throws IOException {
        while (true) {
            try {
                controller.unlink(name, options);
                return;
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    void close(final Closeable closeable) throws IOException {
        while (true) {
            try {
                closeable.close();
                return;
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    private final class SyncInputStream
    extends DecoratingInputStream {
        SyncInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(in);
        }
    } // SyncInputStream

    private final class SyncOutputStream
    extends DecoratingOutputStream {
        SyncOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(out);
        }
    } // SyncOutputStream

    private final class SyncSeekableChannel
    extends DecoratingSeekableChannel {
        SyncSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(channel);
        }
    } // SyncSeekableChannel
}
