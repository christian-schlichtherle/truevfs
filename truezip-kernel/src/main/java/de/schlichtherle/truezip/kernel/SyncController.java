/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsSyncOption.WAIT_CLOSE_IO;
import static de.truezip.kernel.FsSyncOptions.RESET;
import static de.truezip.kernel.FsSyncOptions.SYNC;
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
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Performs a {@link FsController#sync(BitField) sync} operation if required.
 * <p>
 * This controller is a barrier for {@link NeedsSyncException}s:
 * Whenever the decorated controller chain throws a {@code NeedsSyncException},
 * the file system gets {@link #sync(NeedsSyncException) synced} before the
 * operation gets retried.
 * 
 * @see    NeedsSyncException
 * @author Christian Schlichtherle
 */
@Immutable
final class SyncController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private static final BitField<FsSyncOption> NOT_WAIT_CLOSE_IO
            = BitField.of(WAIT_CLOSE_IO).not();

    SyncController(FsController<? extends LockModel> controller) {
        super(controller);
    }

    /**
     * Syncs this controller.
     * 
     * @param  trigger the triggering exception.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    final void sync(final NeedsSyncException trigger)
    throws FsSyncWarningException, FsSyncException {
        checkWriteLockedByCurrentThread();
        try {
            sync(SYNC);
        } catch (final FsSyncException ex) {
            ex.addSuppressed(trigger);
            throw ex;
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
    public FsEntry entry(final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return controller.entry(name);
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
                        return new SyncInputStream(getBoundSocket().stream());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                while (true) {
                    try {
                        return new SyncSeekableChannel(getBoundSocket().channel());
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
                        return new SyncSeekableChannel(getBoundSocket().channel());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public OutputStream stream() throws IOException {
                while (true) {
                    try {
                        return new SyncOutputStream(getBoundSocket().stream());
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
        assert isWriteLockedByCurrentThread();

        while (true) {
            try {
                // HC SUNT DRACONES!
                controller.unlink(name, options); // repeatable for root entry
                if (name.isRoot()) {
                    // Make the file system controller chain eligible for GC.
                    controller.sync(RESET);
                }
                return;
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        final BitField<FsSyncOption> modified = modify(options);
        try {
            controller.sync(modified);
        } catch (final FsSyncWarningException ex) {
            throw ex; // may be FORCE_CLOSE_(IN|OUT)PUT was set, too?
        } catch (final FsSyncException ex) {
            if (modified != options)
                if (ex.getCause() instanceof FsResourceOpenException)
                    throw NeedsLockRetryException.get();
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
        final boolean isRecursive = 1 < LockingStrategy.getLockCount();
        final BitField<FsSyncOption> result = isRecursive
                ? options.and(NOT_WAIT_CLOSE_IO)
                : options;
        assert result == options == result.equals(options) : "Broken contract in BitField.and()!";
        assert result == options || isRecursive;
        return result;
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
