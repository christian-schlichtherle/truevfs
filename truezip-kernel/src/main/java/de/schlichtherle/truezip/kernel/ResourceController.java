/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.FsResourceOpenException;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.FsSyncOption;
import static de.truezip.kernel.FsSyncOption.FORCE_CLOSE_IO;
import static de.truezip.kernel.FsSyncOption.WAIT_CLOSE_IO;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
 * @see    ResourceAccountant
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class ResourceController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private @CheckForNull ResourceAccountant accountant;

    /**
     * Constructs a new file system resource controller.
     *
     * @param controller the decorated file system controller.
     */
    ResourceController(FsController<? extends LockModel> controller) {
        super(controller);
    }

    private ResourceAccountant getAccountant() {
        assert isWriteLockedByCurrentThread();
        final ResourceAccountant a = accountant;
        return null != a ? a : (accountant = new ResourceAccountant(writeLock()));
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
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        waitIdle(options, handler);
        closeAll(handler);
        controller.sync(options, handler);
    }

    /**
     * Waits for all entry input and output resources to close or forces
     * them to close, dependending on the {@code options}.
     * Mind that this method deliberately handles entry input and output
     * streams equally because {@link ResourceAccountant#waitForeignResources}
     * WILL NOT WORK if any two resource accountants share the same lock!
     *
     * @param  options a bit field of synchronization options.
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void
    waitIdle(   final BitField<FsSyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        // HC SUNT DRACONES!
        final ResourceAccountant a = accountant;
        if (null == a)
            return;
        final boolean force = options.get(FORCE_CLOSE_IO);
        final int local = a.localResources();
        final IOException cause;
        if (0 != local && !force) {
            cause = new FsResourceOpenException(a.totalResources(), local);
            throw handler.fail(new FsSyncException(getModel(), cause));
        }
        final boolean wait = options.get(WAIT_CLOSE_IO);
        final int total = a.waitForeignResources(wait ? 0 : WAIT_TIMEOUT_MILLIS);
        if (0 == total)
            return;
        cause = new FsResourceOpenException(total, local);
        if (!force)
            throw handler.fail(new FsSyncException(getModel(), cause));
        handler.warn(new FsSyncWarningException(getModel(), cause));
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void
    closeAll(final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final class IOExceptionHandler
        implements ExceptionHandler<IOException, X> {
            @Override
            public X fail(IOException shouldNotHappen) {
                throw new AssertionError(shouldNotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                assert !(cause instanceof ControlFlowIOException);
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // IOExceptionHandler

        final ResourceAccountant acc = accountant;
        if (null != acc)
            acc.closeAllResources(new IOExceptionHandler());
    }

    private final class ResourceInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            in.close();
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            out.close();
        }
    } // ResourceOutputStream

    private final class ResourceSeekableChannel
    extends DecoratingSeekableChannel {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            channel.close();
        }
    } // ResourceSeekableChannel
}
