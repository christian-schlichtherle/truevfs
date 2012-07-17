/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsResourceAccountant.Resources;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
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
import de.schlichtherle.truezip.util.ControlFlowException;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
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
 * @see    FsResourceAccountant
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsResourceController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final FsResourceAccountant accountant =
            new FsResourceAccountant(writeLock());

    FsResourceController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this, name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this, name, options, template);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        assert isWriteLockedByCurrentThread();
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        waitIdle(options, builder);
        closeAll(builder);
        try {
            delegate.sync(options);
        } catch (final FsSyncException ex) {
            builder.warn(ex);
        }
        builder.check();
    }

    private void waitIdle(
            final BitField<FsSyncOption> options,
            final FsSyncExceptionBuilder builder) throws FsSyncException {
        try {
            waitIdle(options);
        } catch (final FsResourceOpenException ex) {
            if (!options.get(FORCE_CLOSE_INPUT) && !options.get(FORCE_CLOSE_OUTPUT))
                throw builder.fail(new FsSyncException(getModel(), ex));
            builder.warn(new FsSyncWarningException(getModel(), ex));
        }
    }

    private void waitIdle(final BitField<FsSyncOption> options)
    throws FsResourceOpenException {
        // HC SVNT DRACONES!
        final boolean force = options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
        {
            final Resources r = accountant.resources();
            if (0 != r.local && !force) {
                throw new FsResourceOpenException(r.total, r.local);
            }
        }
        final boolean wait = options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
        accountant.waitOtherThreads(wait ? 0 : WAIT_TIMEOUT_MILLIS);
        {
            final Resources r = accountant.resources();
            if (0 != r.total) throw new FsResourceOpenException(r.total, r.local);
        }
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param builder the exception handling strategy.
     */
    private void closeAll(final FsSyncExceptionBuilder builder) {
        final class IOExceptionHandler
        implements ExceptionHandler<IOException, RuntimeException> {
            @Override
            public RuntimeException fail(final IOException ex) {
                throw new AssertionError(ex);
            }

            @Override
            public void warn(final IOException ex) {
                builder.warn(new FsSyncWarningException(getModel(), ex));
            }
        } // IOExceptionHandler
        accountant.closeAllResources(new IOExceptionHandler());
    }

    /**
     * Close()s the given {@code resource} and finally stops accounting for the
     * given {@code account} unless a {@link ControlFlowException} is thrown.
     * 
     * @param  delegate the resource to close().
     * @param  thiz the resource to eventually stop accounting for.
     * @throws IOException on any I/O error.
     * @see http://java.net/jira/browse/TRUEZIP-279 .
     */
    private void close(final Closeable delegate, final Closeable thiz)
    throws IOException {
        boolean cfe = false;
        try {
            delegate.close();
        } catch (final ControlFlowException ex) {
            cfe = true;
            throw ex;
        } finally {
            if (!cfe) accountant.stopAccountingFor(thiz);
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Nio2Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Nio2Output(name, options, template);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsResourceController controller,
                FsEntryName name,
                BitField<FsInputOption> options);
        
        abstract OutputSocket<?> newOutputSocket(
                FsResourceController controller,
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
            return new ResourceSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    @Immutable
    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsResourceController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new ResourceReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new ResourceInputStream(
                    getBoundSocket().newInputStream());
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
            return new ResourceSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    @Immutable
    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(FsResourceController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new ResourceOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class ResourceReadOnlyFile
    extends DecoratingReadOnlyFile {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            close(delegate, this);
        }
    } // ResourceReadOnlyFile

    private final class ResourceSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            close(delegate, this);
        }
    } // ResourceSeekableByteChannel

    private final class ResourceInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            close(delegate, this);
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            close(delegate, this);
        }
    } // ResourceOutputStream
}
