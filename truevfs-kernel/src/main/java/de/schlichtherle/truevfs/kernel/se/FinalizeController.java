/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import de.schlichtherle.truevfs.kernel.ControlFlowException;
import net.truevfs.kernel.*;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class FinalizeController
extends FsDecoratingController<FsModel, FsController<?>> {

    private static final Logger logger = Logger.getLogger(
            FinalizeController.class.getName(),
            FinalizeController.class.getName());

    private static final IOException OK = new IOException((Throwable) null);

    FinalizeController(FsController<?> controller) {
        super(controller);
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
                return new FinalizeInputStream(boundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new FinalizeSeekableChannel(boundSocket().channel());
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
                return new FinalizeOutputStream(boundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new FinalizeSeekableChannel(boundSocket().channel());
            }
        } // Output

        return new Output();
    }

    static void finalize(   final Closeable closeable,
                            final @CheckForNull IOException close) {
        if (OK == close) {
            logger.log(Level.FINEST, "closeCleared");
        } else if (null != close) {
            logger.log(Level.FINER, "closeFailed", close);
        } else {
            try {
                closeable.close();
                logger.log(Level.FINE, "finalizeCleared");
            } catch (final ControlFlowException ex) {  // report and swallow
                logger.log(Level.WARNING, "finalizeFailed",
                        new AssertionError("Unexpected control flow exception!", ex));
            } catch (final Throwable ex) {              // report and swallow
                logger.log(Level.INFO, "finalizeFailed", ex);
            }
        }
    }

    @NotThreadSafe
    private static final class FinalizeInputStream
    extends DecoratingInputStream {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            try {
                in.close();
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        @DischargesObligation
        protected void finalize() throws Throwable {
            try {
                finalize(in, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeInputStream

    @NotThreadSafe
    private static final class FinalizeOutputStream
    extends DecoratingOutputStream {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            try {
                out.close();
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        @DischargesObligation
        protected void finalize() throws Throwable {
            try {
                finalize(out, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeOutputStream

    @NotThreadSafe
    private static final class FinalizeSeekableChannel
    extends DecoratingSeekableChannel {
        volatile IOException close; // accessed by finalizer thread!

        FinalizeSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            try {
                channel.close();
            } catch (final IOException ex) {
                throw close = ex;
            }
            close = OK;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        @DischargesObligation
        protected void finalize() throws Throwable {
            try {
                finalize(channel, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeSeekableChannel
}
