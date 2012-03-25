/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.cio.*;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.fs.option.FsOutputOption;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableByteChannel;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 * 
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsFinalizeController
extends FsDecoratingController<FsModel, FsController<?>> {

    private static final Logger logger = Logger.getLogger(
            FsFinalizeController.class.getName(),
            FsFinalizeController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private static final IOException OK = new IOException((Throwable) null);

    /**
     * Constructs a new file system finalize controller.
     *
     * @param controller the decorated file system controller.
     */
    FsFinalizeController(FsController<?> controller) {
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

    static void finalize(   final Closeable delegate,
                            final @CheckForNull IOException close) {
        if (OK == close) {
            logger.log(Level.FINEST, "closeCleared");
        } else if (null != close) {
            logger.log(Level.FINER, "closeFailed", close);
        } else {
            try {
                delegate.close();
                logger.log(Level.FINE, "finalizeCleared");
            } catch (final FsControllerException ex) {  // report and swallow
                logger.log(Level.WARNING, "finalizeFailed",
                        new AssertionError("Unexpected controller exception!", ex));
            } catch (final Throwable ex) {              // report and swallow
                logger.log(Level.INFO, "finalizeFailed", ex);
            }
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsFinalizeController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Nio2Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsFinalizeController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Nio2Output(name, options, template);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsFinalizeController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsFinalizeController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsFinalizeController controller,
                FsEntryName name,
                BitField<FsInputOption> options);
        
        abstract OutputSocket<?> newOutputSocket(
                FsFinalizeController controller,
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
            return new FinalizeSeekableByteChannel(
                    getBoundDelegate().newSeekableByteChannel());
        }
    } // Nio2Input

    @Immutable
    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsFinalizeController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new FinalizeReadOnlyFile(
                    getBoundDelegate().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new FinalizeInputStream(
                    getBoundDelegate().newInputStream());
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
            return new FinalizeSeekableByteChannel(
                    getBoundDelegate().newSeekableByteChannel());
        }
    } // Nio2Output

    @Immutable
    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(FsFinalizeController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION") // false positive
        public OutputStream newOutputStream() throws IOException {
            return new FinalizeOutputStream(
                    getBoundDelegate().newOutputStream());
        }
    } // Output

    private static final class FinalizeReadOnlyFile
    extends DecoratingReadOnlyFile {
        volatile IOException close; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } catch (final FsControllerException ex) {
                assert ex instanceof FsNeedsLockRetryException : ex;
                // This is a non-local control flow exception.
                // This call may or may not get retried again later.
                // Do NOT record the status so that finalize() will call close()
                // on the decorated resource if this call is NOT retried again.
                throw ex;
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeReadOnlyFile

    private static final class FinalizeSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        volatile IOException close; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } catch (final FsControllerException ex) {
                assert ex instanceof FsNeedsLockRetryException : ex;
                // This is a non-local control flow exception.
                // This call may or may not get retried again later.
                // Do NOT record the status so that finalize() will call close()
                // on the decorated resource if this call is NOT retried again.
                throw ex;
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeSeekableByteChannel

    private static final class FinalizeInputStream
    extends DecoratingInputStream {
        volatile IOException close; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } catch (final FsControllerException ex) {
                assert ex instanceof FsNeedsLockRetryException : ex;
                // This is a non-local control flow exception.
                // This call may or may not get retried again later.
                // Do NOT record the status so that finalize() will call close()
                // on the decorated resource if this call is NOT retried again.
                throw ex;
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeInputStream

    private static final class FinalizeOutputStream
    extends DecoratingOutputStream {
        volatile IOException close; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } catch (final FsControllerException ex) {
                assert ex instanceof FsNeedsLockRetryException : ex;
                // This is a non-local control flow exception.
                // This call may or may not get retried again later.
                // Do NOT record the status so that finalize() will call close()
                // on the decorated resource if this call is NOT retried again.
                throw ex;
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeOutputStream
}
