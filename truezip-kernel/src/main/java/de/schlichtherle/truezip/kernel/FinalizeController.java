/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDecoratingController;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.util.BitField;
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
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FinalizeController
extends FsDecoratingController<FsModel, FsController<?>> {

    private static final Logger logger = Logger.getLogger(
            FinalizeController.class.getName(),
            FinalizeController.class.getName());

    private static final IOException OK = new IOException((Throwable) null);

    /**
     * Constructs a new file system finalize controller.
     *
     * @param controller the decorated file system controller.
     */
    FinalizeController(FsController<?> controller) {
        super(controller);
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
                return new FinalizeInputStream(getBoundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new FinalizeSeekableChannel(getBoundSocket().channel());
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
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION") // false positive
            public OutputStream stream() throws IOException {
                return new FinalizeOutputStream(getBoundSocket().stream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new FinalizeSeekableChannel(getBoundSocket().channel());
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
            } catch (final FsControlFlowIOException ex) {  // report and swallow
                logger.log(Level.WARNING, "finalizeFailed",
                        new AssertionError("Unexpected controller exception!", ex));
            } catch (final Throwable ex) {              // report and swallow
                logger.log(Level.INFO, "finalizeFailed", ex);
            }
        }
    }

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
                in.close();
            } catch (final FsControlFlowIOException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
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
                finalize(in, close);
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
                out.close();
            } catch (final FsControlFlowIOException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
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
                finalize(out, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeOutputStream

    private static final class FinalizeSeekableChannel
    extends DecoratingSeekableChannel {
        volatile IOException close; // accessed by finalizer thread!

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        FinalizeSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public void close() throws IOException {
            try {
                channel.close();
            } catch (final FsControlFlowIOException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
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
                finalize(channel, close);
            } finally {
                super.finalize();
            }
        }
    } // FinalizeSeekableChannel
}
