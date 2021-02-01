/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.xz;

import global.namespace.truevfs.comp.cio.InputContainer;
import global.namespace.truevfs.comp.cio.OutputContainer;
import global.namespace.truevfs.comp.io.AbstractSink;
import global.namespace.truevfs.comp.io.AbstractSource;
import global.namespace.truevfs.comp.io.Streams;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.tardriver.*;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.cio.MultiplexingOutputContainer;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import javax.annotation.CheckForNull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static global.namespace.truevfs.kernel.api.FsAccessOption.STORE;

/**
 * An archive driver for XZ compressed TAR files (TAR.XZ).
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
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
    protected InputContainer<TarDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource source)
            throws IOException {

        class Source extends AbstractSource {

            @Override
            public InputStream stream() throws IOException {
                final InputStream in = source.stream();
                try {
                    return new XZInputStream(
                            new BufferedInputStream(in, getBufferSize()));
                } catch (final Throwable t1) {
                    try {
                        in.close();
                    } catch (final Throwable t2) {
                        t1.addSuppressed(t2);
                    }
                    throw t1;
                }
            }
        }

        return new TarInputContainer(model, new Source(), this);
    }

    @Override
    protected OutputContainer<TarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputContainer<TarDriverEntry> input)
            throws IOException {

        class Sink extends AbstractSink {

            @Override
            public OutputStream stream() throws IOException {
                final OutputStream out = sink.stream();
                try {
                    return new FixedXZOutputStream(
                            new FixedBufferedOutputStream(out, getBufferSize()),
                            new LZMA2Options(getPreset()));
                } catch (final Throwable t1) {
                    try {
                        out.close();
                    } catch (Throwable t2) {
                        t1.addSuppressed(t2);
                    }
                    throw t1;
                }
            }
        }

        return new MultiplexingOutputContainer<>(getPool(), new TarOutputContainer(model, new Sink(), this));
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        return new FsOutputSocketSink(options, controller.output(options, name, Optional.empty()));
    }

    private static final class FixedXZOutputStream extends XZOutputStream {

        final OutputStream out;
        boolean closed;

        FixedXZOutputStream(final OutputStream out, final LZMA2Options options) throws IOException {
            super(out, options);
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                out.close(); // enable recovery
            } else {
                closed = true;
                super.close();
            }
        }
    }
}
