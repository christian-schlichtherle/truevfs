/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.io.DecoratingInputStream;
import global.namespace.truevfs.comp.io.DecoratingOutputStream;
import global.namespace.truevfs.comp.io.DecoratingSeekableChannel;
import global.namespace.truevfs.comp.logging.LocalizedLogger;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.ControlFlowException;
import global.namespace.truevfs.kernel.api.FsAccessOption;
import global.namespace.truevfs.kernel.api.FsDelegatingController;
import global.namespace.truevfs.kernel.api.FsNodeName;
import lombok.val;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
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
abstract class FinalizeController implements FsDelegatingController {

    private static final Logger logger = new LocalizedLogger(FinalizeController.class);

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new DecoratingInputSocket<Entry>() {

            {
                socket = getController().input(options, name);
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return new FinalizeInputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
                    throws IOException {
                return new FinalizeSeekableChannel(socket.channel(peer));
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template) {
        return new DecoratingOutputSocket<Entry>() {

            {
                socket = getController().output(options, name, template);
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return new FinalizeOutputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer)
                    throws IOException {
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class FinalizeCloseable implements Closeable {

        final Closeable closeable;

        // Accessed by finalizer thread:
        volatile @CheckForNull Optional<IOException> ioException;

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
