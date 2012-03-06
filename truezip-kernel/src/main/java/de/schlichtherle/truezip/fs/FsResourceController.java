/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
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
extends FsLockModelDecoratingController<
        FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private @CheckForNull FsResourceAccountant accountant;

    /**
     * Constructs a new file system resource controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsResourceController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    private FsResourceAccountant getAccountant() {
        assert isWriteLockedByCurrentThread();
        final FsResourceAccountant acc = accountant;
        return null != acc
                ? acc
                : (accountant = new FsResourceAccountant(writeLock()));
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
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
     * streams equally because {@link FsResourceAccountant#waitForeignResources}
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
        final FsResourceAccountant acc = accountant;
        if (null == acc)
            return;
        final boolean force = options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
        final int local = acc.localResources();
        final IOException cause;
        if (0 != local && !force) {
            cause = new FsResourceBusyIOException(acc.totalResources(), local);
            throw handler.fail(new FsSyncException(getModel(), cause));
        }
        final boolean wait = options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
        final int total = acc.waitForeignResources(
                wait ? 0 : WAIT_TIMEOUT_MILLIS);
        if (0 == total)
            return;
        cause = new FsResourceBusyIOException(total, local);
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
        class IOExceptionHandler
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

        final FsResourceAccountant acc = accountant;
        if (null != acc)
            acc.closeAllResources(new IOExceptionHandler());
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsResourceController controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsResourceController controller,
                OutputSocket <?> output);
    } // SocketFactory

    private final class Nio2Input extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new ResourceSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
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

    private final class Nio2Output extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new ResourceSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    private class Output extends DecoratingOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
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
            assert isWriteLockedByCurrentThread();
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }
    } // ResourceReadOnlyFile

    private final class ResourceSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
            assert isWriteLockedByCurrentThread();
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }
    } // ResourceSeekableByteChannel

    private final class ResourceInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
            assert isWriteLockedByCurrentThread();
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
            assert isWriteLockedByCurrentThread();
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }
    } // ResourceOutputStream
}
