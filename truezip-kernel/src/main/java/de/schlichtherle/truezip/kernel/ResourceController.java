/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsSyncOption.FORCE_CLOSE_IO;
import static de.truezip.kernel.FsSyncOption.WAIT_CLOSE_IO;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Accounts input and output resources returned by its decorated controller.
 * 
 * @see    ResourceManager
 * @author Christian Schlichtherle
 */
@Immutable
final class ResourceController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private static final int
            WAIT_TIMEOUT_MILLIS = LockingStrategy.ACQUIRE_TIMEOUT_MILLIS;

    private final ResourceManager manager;

    ResourceController(FsController<? extends LockModel> controller) {
        super(controller);
        this.manager = new ResourceManager(writeLock());
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
            public InputStream stream() throws IOException {
                return new ResourceInputStream(getBoundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new ResourceSeekableChannel(getBoundSocket().channel());
            }
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive
    public OutputSocket<?> output(
            final BitField<FsAccessOption> options, final FsEntryName name, @CheckForNull
    final Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(options, name, template));
            }

            @Override
            public OutputStream stream() throws IOException {
                return new ResourceOutputStream(getBoundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new ResourceSeekableChannel(getBoundSocket().channel());
            }
        } // Output

        return new Output();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        assert isWriteLockedByCurrentThread();

        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        waitIdle(options, builder);
        closeAll(builder);
        try {
            controller.sync(options);
        } catch (final FsSyncException ex) {
            builder.warn(ex);
        }
        builder.check();
    }

    private void waitIdle(  final BitField<FsSyncOption> options,
                            final FsSyncExceptionBuilder builder)
    throws FsSyncException {
        try {
            waitIdle(options);
        } catch (final FsResourceOpenException ex) {
            if (!options.get(FORCE_CLOSE_IO))
                throw builder.fail(new FsSyncException(getModel(), ex));
            builder.warn(new FsSyncWarningException(getModel(), ex));
        }
    }

    private void waitIdle(final BitField<FsSyncOption> options)
    throws FsResourceOpenException {
        // HC SUNT DRACONES!
        final ResourceManager manager = this.manager;
        final int local = manager.localResources();
        if (0 != local && !options.get(FORCE_CLOSE_IO))
            throw new FsResourceOpenException(manager.totalResources(), local);
        final boolean wait = options.get(WAIT_CLOSE_IO);
        if (!wait) {
            // Spend some effort on closing streams which have already been
            // garbage collected in order to compensates for a disadvantage of
            // the NeedsLockRetryException:
            // An FsArchiveDriver may try to close() a file system entry but
            // fail to do so because of a NeedsLockRetryException which is
            // impossible to resolve in a driver.
            // The TarDriver family is known to be affected by this.
            System.runFinalization();
        }
        final int total = manager.waitOtherThreads(
                wait ? 0 : WAIT_TIMEOUT_MILLIS);
        if (0 != total)
            throw new FsResourceOpenException(total, local);
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     * 
     * @param  builder the exception handling strategy.
     */
    private void closeAll(final FsSyncExceptionBuilder builder) {
        try {
            manager.closeAllResources();
        } catch (final IOException ex) {
            builder.warn(new FsSyncWarningException(getModel(), ex));
        }
    }

    private final class ResourceInputStream
    extends DecoratingInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
            manager.start(this);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            in.close();
            manager.stop(this);
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
            manager.start(this);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            out.close();
            manager.stop(this);
        }
    } // ResourceOutputStream

    private final class ResourceSeekableChannel
    extends DecoratingSeekableChannel {
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
            manager.start(this);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            channel.close();
            manager.stop(this);
        }
    } // ResourceSeekableChannel
}
