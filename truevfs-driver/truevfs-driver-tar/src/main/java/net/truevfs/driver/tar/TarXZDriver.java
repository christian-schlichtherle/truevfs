/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;
import static net.truevfs.kernel.spec.FsAccessOption.STORE;
import net.truevfs.kernel.spec.*;
import net.truevfs.kernel.spec.cio.InputService;
import net.truevfs.kernel.spec.cio.MultiplexingOutputService;
import net.truevfs.kernel.spec.cio.OutputService;
import net.truevfs.kernel.spec.io.AbstractSink;
import net.truevfs.kernel.spec.io.AbstractSource;
import net.truevfs.kernel.spec.io.Streams;
import net.truevfs.kernel.spec.util.BitField;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * An archive driver for XZ compressed TAR files (TAR.XZ).
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class TarXZDriver extends TarDriver {

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarXZDriver} returns
     * {@link Streams#BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return Streams.BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing an XZ output stream.
     * <p>
     * The implementation in the class {@link TarXZDriver} returns
     * {@link LZMA2Options#PRESET_DEFAULT}.
     * 
     * @return The compression level to use when writing a XZ output stream.
     */
    public int getPreset() {
        return LZMA2Options.PRESET_DEFAULT;
    }

    @Override
    protected InputService<TarDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource source)
    throws IOException {
        final class Source extends AbstractSource {
            @Override
            public InputStream stream() throws IOException {
                final InputStream in = source.stream();
                try {
                    return new XZInputStream(
                            new BufferedInputStream(in, getBufferSize()));
                } catch (final Throwable ex) {
                    try {
                        in.close();
                    } catch (final Throwable ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
        } // Source
        return new TarInputService(model, new Source(), this);
    }

    @Override
    protected OutputService<TarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final InputService<TarDriverEntry> input)
    throws IOException {
        final class Sink extends AbstractSink {
            @Override
            public OutputStream stream() throws IOException {
                final OutputStream out = sink.stream();
                try {
                    return new FixedXZOutputStream(
                            new FixedBufferedOutputStream(out, getBufferSize()),
                            new LZMA2Options(getPreset()));
                } catch (final Throwable ex) {
                    try {
                        out.close();
                    } catch (final Throwable ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
        } // Sink
        return new MultiplexingOutputService<>(getIoPool(),
                new TarOutputService(model, new Sink(), this));
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        return new FsOutputSocketSink(options,
                controller.output(options, name, null));
    }

    private static final class FixedXZOutputStream extends XZOutputStream {
        final FixedBufferedOutputStream out;

        private FixedXZOutputStream(
                final FixedBufferedOutputStream out,
                final LZMA2Options options)
        throws IOException {
            super(out, options);
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            // Workaround for super class implementation which remembers and
            // rethrows any IOException thrown by the decorated output stream.
            // Unfortunately, this doesn't work with TrueZIP's
            // FsControllerException, which is an IOException.
            // TODO: Remove all this in TrueVFS. TrueVFS uses a
            // ControlFlowException instead, which is a RuntimeException and
            // should not interfere with the super class implementation in this
            // way.
            out.ignoreClose = true;
            super.close();
            out.ignoreClose = false;
            out.close();
        }
    } // FixedXZOutputStream
}
