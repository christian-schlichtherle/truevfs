/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.LockManagement.WAIT_TIMEOUT_MILLIS;
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
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Accounts input and output resources returned by its decorated controller.
 * 
 * @see    ResourceManager
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class ResourceController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private final ResourceManager manager;

    /**
     * Constructs a new file system resource controller.
     *
     * @param controller the decorated file system controller.
     */
    ResourceController(FsController<? extends LockModel> controller) {
        super(controller);
        this.manager = new ResourceManager(writeLock());
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
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.output(name, options, template));
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

    /**
     * Waits for all entry input and output resources to close or forces
     * them to close, dependending on the {@code options}.
     * Mind that this method deliberately handles entry input and output
     * streams equally because {@link ResourceManager#waitForeignResources}
     * WILL NOT WORK if any two resource accountants share the same lock!
     *
     * @param  options a bit field of synchronization options.
     * @param  builder the exception handling strategy.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private void waitIdle(  final BitField<FsSyncOption> options,
                            final FsSyncExceptionBuilder builder)
    throws FsSyncException {
        // HC SUNT DRACONES!
        final ResourceManager manager = this.manager;
        final boolean force = options.get(FORCE_CLOSE_IO);
        final int local = manager.localResources();
        final IOException ex;
        if (0 != local && !force) {
            ex = new FsResourceOpenException(manager.totalResources(), local);
            throw builder.fail(new FsSyncException(getModel(), ex));
        }
        final boolean wait = options.get(WAIT_CLOSE_IO);
        final int total = manager.waitForeignResources(
                wait ? 0 : WAIT_TIMEOUT_MILLIS);
        if (0 == total)
            return;
        ex = new FsResourceOpenException(total, local);
        if (!force)
            throw builder.fail(new FsSyncException(getModel(), ex));
        builder.warn(new FsSyncWarningException(getModel(), ex));
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     * 
     * @param  builder the exception handling strategy.
     */
    private void closeAll(final FsSyncExceptionBuilder builder) {
        final ResourceManager manager = this.manager;
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
            manager.stop(this);
            in.close();
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
            manager.stop(this);
            out.close();
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
            manager.stop(this);
            channel.close();
        }
    } // ResourceSeekableChannel
}
