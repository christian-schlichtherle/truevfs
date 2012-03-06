/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
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
import de.schlichtherle.truezip.util.JSE7;
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
 * @param  <M> the type of the file system model.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsFinalizeController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final Logger logger = Logger.getLogger(
            FsFinalizeController.class.getName(),
            FsFinalizeController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private static final IOException OK = new IOException();

    /**
     * Constructs a new file system finalize controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsFinalizeController(FsController<? extends M> controller) {
        super(controller);
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

    static void finalize(   final Closeable delegate,
                            final @CheckForNull IOException status) {
        if (OK.equals(status)) {
            logger.log(Level.FINEST, "closeCleared");
        } else if (null != status) {
            logger.log(Level.FINER, "closeFailed", status);
        } else {
            try {
                delegate.close();
                logger.log(Level.FINE, "finalizeCleared");
            } catch (final Throwable ex) {
                logger.log(Level.FINE, "finalizeFailed", ex);
            }
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsFinalizeController<?> controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsFinalizeController<?> controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        },

        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsFinalizeController<?> controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsFinalizeController<?> controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsFinalizeController<?> controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsFinalizeController<?> controller,
                OutputSocket <?> output);
    } // SocketFactory

    private final class Nio2Input extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new FinalizeSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new FinalizeReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new FinalizeInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private final class Nio2Output extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new FinalizeSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    private class Output extends DecoratingOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new FinalizeOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class FinalizeReadOnlyFile
    extends DecoratingReadOnlyFile {
        volatile IOException status; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                status = OK;
            } catch (final IOException ex) {
                throw status = ex;
            }
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, status);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeReadOnlyFile

    private final class FinalizeSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        volatile IOException status; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                status = OK;
            } catch (final IOException ex) {
                throw status = ex;
            }
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, status);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeSeekableByteChannel

    private final class FinalizeInputStream
    extends DecoratingInputStream {
        volatile IOException status; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                status = OK;
            } catch (final IOException ex) {
                throw status = ex;
            }
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, status);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeInputStream

    private final class FinalizeOutputStream
    extends DecoratingOutputStream {
        volatile IOException status; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                status = OK;
            } catch (final IOException ex) {
                throw status = ex;
            }
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, status);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeOutputStream
}
