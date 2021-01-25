/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.bzip2;

import net.java.truecommons.cio.InputService;
import net.java.truecommons.cio.OutputService;
import net.java.truecommons.io.AbstractSink;
import net.java.truecommons.io.AbstractSource;
import net.java.truecommons.io.Streams;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.comp.tardriver.*;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import java.io.*;

import static net.java.truevfs.kernel.spec.FsAccessOption.STORE;

/**
 * An archive driver for BZIP2 compressed TAR files (TAR.BZIP2).
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class TarBZip2Driver extends TarDriver {

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link Streams#BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return Streams.BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a BZIP2 sink stream.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream#MAX_BLOCKSIZE}.
     *
     * @return The compression level to use when writing a BZIP2 sink stream.
     */
    public int getLevel() {
        return BZip2CompressorOutputStream.MAX_BLOCKSIZE;
    }

    @Override
    protected InputService<TarDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource source)
            throws IOException {

        class Source extends AbstractSource {

            @Override
            public InputStream stream() throws IOException {
                final InputStream in = source.stream();
                try {
                    return new BZip2CompressorInputStream(
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

        return new TarInputService(model, new Source(), this);
    }

    @Override
    protected OutputService<TarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<TarDriverEntry> input)
            throws IOException {

        class Sink extends AbstractSink {

            @Override
            public OutputStream stream() throws IOException {
                final OutputStream out = sink.stream();
                try {
                    return new FixedBZip2CompressorOutputStream(
                            new FixedBufferedOutputStream(out, getBufferSize()),
                            getLevel());
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

        return new MultiplexingOutputService<>(getPool(), new TarOutputService(model, new Sink(), this));
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
        return new FsOutputSocketSink(options, controller.output(options, name, null));
    }

    private static final class FixedBZip2CompressorOutputStream extends BZip2CompressorOutputStream {

        final OutputStream out;
        boolean closed;

        FixedBZip2CompressorOutputStream(final OutputStream out, final int level) throws IOException {
            super(out, level);
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
