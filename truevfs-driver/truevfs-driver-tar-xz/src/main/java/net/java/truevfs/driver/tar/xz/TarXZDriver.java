/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.xz;

import net.java.truecommons.cio.InputService;
import net.java.truecommons.cio.OutputService;
import net.java.truecommons.io.AbstractSink;
import net.java.truecommons.io.AbstractSource;
import net.java.truecommons.io.Streams;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.comp.tardriver.*;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import javax.annotation.concurrent.Immutable;
import java.io.*;

import static net.java.truevfs.kernel.spec.FsAccessOption.STORE;

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
        return new MultiplexingOutputService<>(getPool(),
                new TarOutputService(model, new Sink(), this));
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
        return new FsOutputSocketSink(options,
                controller.output(options, name, null));
    }

    private static final class FixedXZOutputStream extends XZOutputStream {

        final FixedBufferedOutputStream out;

        FixedXZOutputStream(final FixedBufferedOutputStream out, final LZMA2Options options) throws IOException {
            super(out, options);
            this.out = out;
        }

        //
        // Ignores the call.
        // This workaround is required for proper error recovery in Java 8, where {@link FilterOutputStream#close()}
        // no longer silently ignores any {@link IOException} thrown by {@link FilterOutputStream#flush()}.
        //
        @Override
        public void flush() throws IOException { }

        @Override
        public void close() throws IOException {
            // Workaround for super class implementation which fails to close the decorated stream on a subsequent call
            // if the initial attempt failed with a throwable - see http://java.net/jira/browse/TRUEZIP-234 .
            out.setIgnoreClose(true);
            super.close();
            out.setIgnoreClose(false);
            out.close();
        }
    }
}
