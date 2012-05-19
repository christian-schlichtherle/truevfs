/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import de.schlichtherle.truevfs.kernel.NeedsLockRetryException;
import de.schlichtherle.truevfs.kernel.NeedsSyncException;
import static net.truevfs.kernel.FsSyncOption.WAIT_CLOSE_IO;
import static net.truevfs.kernel.FsSyncOptions.RESET;
import static net.truevfs.kernel.FsSyncOptions.SYNC;
import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Entry.Access;
import net.truevfs.kernel.cio.Entry.Type;
import net.truevfs.kernel.cio.*;
import net.truevfs.kernel.io.DecoratingInputStream;
import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.io.DecoratingSeekableChannel;
import net.truevfs.kernel.util.BitField;
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
     * @param opEx the triggering exception from the file system operation.
     */
    final void sync(final NeedsSyncException opEx)
    throws FsSyncWarningException, FsSyncException {
        checkWriteLockedByCurrentThread();
        try {
            sync(SYNC);
        } catch (final FsSyncException syncEx) {
            syncEx.addSuppressed(opEx);
            throw syncEx;
        }
    }

    @Override
    public FsEntry stat(BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return controller.stat(options, name);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types)
    throws IOException {
        while (true) {
            try {
                controller.checkAccess(options, name, types);
                return;
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
            final BitField<FsAccessOption> options, final FsEntryName name, final Map<Access, Long> times)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(options, name, times);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types, final long value)
    throws IOException {
        while (true) {
            try {
                return controller.setTime(options, name, types, value);
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public InputSocket<?> input(
            final BitField<FsAccessOption> options, final FsEntryName name) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.input(options, name));
            }

            @Override
            public Entry localTarget() throws IOException {
                while (true) {
                    try {
                        return boundSocket().localTarget();
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public InputStream stream() throws IOException {
                while (true) {
                    try {
                        return new SyncInputStream(boundSocket().stream());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                while (true) {
                    try {
                        return new SyncSeekableChannel(boundSocket().channel());
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
            final BitField<FsAccessOption> options, final FsEntryName name, @CheckForNull
    final Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(options, name, template));
            }

            @Override
            public Entry localTarget() throws IOException {
                while (true) {
                    try {
                        return boundSocket().localTarget();
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                while (true) {
                    try {
                        return new SyncSeekableChannel(boundSocket().channel());
                    } catch (NeedsSyncException ex) {
                        sync(ex);
                    }
                }
            }

            @Override
            public OutputStream stream() throws IOException {
                while (true) {
                    try {
                        return new SyncOutputStream(boundSocket().stream());
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
            final BitField<FsAccessOption> options, final FsEntryName name, final Type type, @CheckForNull
    final Entry template)
    throws IOException {
        while (true) {
            try {
                controller.mknod(options, name, type, template);
                return;
            } catch (NeedsSyncException ex) {
                sync(ex);
            }
        }
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                // HC SUNT DRACONES!
                controller.unlink(options, name); // repeatable for root entry
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
        SyncSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            close(channel);
        }
    } // SyncSeekableChannel
}
