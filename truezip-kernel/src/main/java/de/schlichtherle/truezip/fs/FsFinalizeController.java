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
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 * 
 * @param  <M> the type of the file system model.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class FsFinalizeController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final Logger logger = Logger.getLogger(
            FsFinalizeController.class.getName(),
            FsFinalizeController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

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
                                            Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    static void finalize(final Closeable delegate, final Boolean closed) {
        Throwable failure = null;
        try {
            if (null == closed) {
                try {
                    delegate.close();
                } catch (Throwable ex) {
                    failure = ex;
                }
            }
        } finally {
            if (TRUE.equals(closed)) {
                logger.log(Level.FINEST, "closeCleared");
            } else if (FALSE.equals(closed)) {
                logger.log(Level.FINER, "closeFailed");
            } else if (null == failure) {
                logger.log(Level.FINE, "finalizeCleared");
            } else {
                logger.log(Level.FINE, "finalizeFailed", failure);
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
        Boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        FinalizeReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            closed = FALSE;
            delegate.close();
            closed = TRUE;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, closed);
            } finally {
                super.finalize();
            }
        }
    } // AccountingReadOnlyFile

    private final class FinalizeSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        Boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        FinalizeSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            closed = FALSE;
            delegate.close();
            closed = TRUE;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, closed);
            } finally {
                super.finalize();
            }
        }
    } // AccountingSeekableByteChannel

    private final class FinalizeInputStream
    extends DecoratingInputStream {
        Boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        FinalizeInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = FALSE;
            delegate.close();
            closed = TRUE;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, closed);
            } finally {
                super.finalize();
            }
        }
    } // AccountingInputStream

    private final class FinalizeOutputStream
    extends DecoratingOutputStream {
        Boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        FinalizeOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closed = FALSE;
            delegate.close();
            closed = TRUE;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                finalize(delegate, closed);
            } finally {
                super.finalize();
            }
        }
    } // AccountingOutputStream
}
