/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.val;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DecoratingSeekableChannel;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.ControlFlowException;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDecoratingController;
import net.java.truevfs.kernel.spec.FsNodeName;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * Finalizes unclosed resources returned by its decorated controller.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FinalizeController extends FsDecoratingController {

    private static final Logger logger = new LocalizedLogger(FinalizeController.class);

    FinalizeController(FsController controller) {
        super(controller);
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new DelegatingInputSocket<Entry>() {

            final InputSocket<? extends Entry> socket = controller.input(options, name);

            @Override
            protected InputSocket<? extends Entry> socket() throws IOException {
                return socket;
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                return new FinalizeInputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                return new FinalizeSeekableChannel(socket.channel(peer));
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, @CheckForNull Entry template) {
        return new DelegatingOutputSocket<Entry>() {

            final OutputSocket<? extends Entry> socket = controller.output(options, name, template);

            @Override
            protected OutputSocket<? extends Entry> socket() throws IOException {
                return socket;
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                return new FinalizeOutputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
                return new FinalizeSeekableChannel(socket.channel(peer));
            }
        };
    }

    private static final class FinalizeInputStream extends DecoratingInputStream {

        final FinalizeCloseable closeable = new FinalizeCloseable(in);

        FinalizeInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }

        @Override
        @Deprecated
        protected void finalize() throws Throwable {
            try {
                closeable.onBeforeFinalize();
            } finally {
                super.finalize();
            }
        }
    }

    private static final class FinalizeOutputStream extends DecoratingOutputStream {

        final FinalizeCloseable closeable = new FinalizeCloseable(out);

        FinalizeOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }

        @Override
        @Deprecated
        protected void finalize() throws Throwable {
            try {
                closeable.onBeforeFinalize();
            } finally {
                super.finalize();
            }
        }
    }

    private static final class FinalizeSeekableChannel extends DecoratingSeekableChannel {

        final FinalizeCloseable closeable = new FinalizeCloseable(channel);

        FinalizeSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }

        @Override
        @Deprecated
        protected void finalize() throws Throwable {
            try {
                closeable.onBeforeFinalize();
            } finally {
                super.finalize();
            }
        }
    }

    private static final class FinalizeCloseable implements Closeable {

        final Closeable closeable;

        // Accessed by finalizer thread:
        @CheckForNull
        volatile Optional<IOException> ioException;

        FinalizeCloseable(final Closeable closeable) {
            this.closeable = closeable;
        }

        @Override
        public final void close() throws IOException {
            try {
                closeable.close();
                ioException = Optional.empty();
            } catch (IOException e) {
                ioException = Optional.of(e);
                throw e;
            }
        }

        void onBeforeFinalize() {
            val e = ioException;
            //noinspection OptionalAssignedToNull
            if (null == e) {
                try {
                    closeable.close();
                    logger.info("finalizeCleared");
                } catch (ControlFlowException t) {
                    // Log and swallow:
                    logger.error("finalizeFailed", new AssertionError("Unexpected control flow exception!", t));
                } catch (Throwable t) {
                    // Log and swallow:
                    logger.warn("finalizeFailed", t);
                }
            } else if (e.isPresent()) {
                logger.trace("closeFailed", e.get());
            } else {
                logger.trace("closeCleared");
            }
        }
    }
}
