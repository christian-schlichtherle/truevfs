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
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
public final class FsResourceController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final FsResourceAccountant accountant =
            new FsResourceAccountant(writeLock());

    public FsResourceController(FsController<? extends FsLockModel> controller) {
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
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        waitIdle(options, handler);
        closeAll(handler);
        delegate.sync(options, handler);
    }

    /**
     * Waits for all entry input and output resources to close or forces
     * them to close, dependending on the {@code options}.
     * Mind that this method deliberately handles entry input and output
     * streams equally because {@link FsResourceAccountant#waitOtherThreads}
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
        // HC SVNT DRACONES!
        final boolean force = options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
        {
            final Resources r = accountant.resources();
            if (0 != r.local && !force) {
                final IOException cause =
                        new FsResourceOpenException(r.total, r.local);
                throw handler.fail(new FsSyncException(getModel(), cause));
            }
        }
        final boolean wait = options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
        if (!wait) {
            // Spend some effort on closing streams which have already been
            // garbage collected in order to compensates for a disadvantage of
            // the FsNeedsLockRetryException:
            // An FsArchiveDriver may try to close() a file system entry but
            // fail to do so because of a FsNeedsLockRetryException which is
            // impossible to resolve in a driver.
            // The TarDriver family is known to be affected by this.
            System.runFinalization();
        }
        accountant.waitOtherThreads(wait ? 0 : WAIT_TIMEOUT_MILLIS);
        {
            final Resources r = accountant.resources();
            if (0 == r.total)
                return;
            final IOException cause =
                    new FsResourceOpenException(r.total, r.local);
            if (!force)
                throw handler.fail(new FsSyncException(getModel(), cause));
            handler.warn(new FsSyncWarningException(getModel(), cause));
        }
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
    throws IOException {
        final class IOExceptionHandler
        implements ExceptionHandler<IOException, X> {
            @Override
            public X fail(IOException shouldNotHappen) {
                throw new AssertionError(shouldNotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                assert !(cause instanceof FsControllerException);
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // IOExceptionHandler

        accountant.closeAllResources(new IOExceptionHandler());
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
            delegate.close();
            accountant.stopAccountingFor(this);
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
            delegate.close();
            accountant.stopAccountingFor(this);
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
            delegate.close();
            accountant.stopAccountingFor(this);
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
            delegate.close();
            accountant.stopAccountingFor(this);
        }
    } // ResourceOutputStream
}
