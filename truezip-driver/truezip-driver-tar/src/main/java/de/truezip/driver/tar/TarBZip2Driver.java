/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsAccessOption;
import static de.truezip.kernel.FsAccessOption.STORE;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.AbstractSource;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.util.BitField;
import java.io.*;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

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
     * The buffer size used for reading and writing.
     * Optimized for performance.
     */
    public static final int BUFFER_SIZE = Streams.BUFFER_SIZE;

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link #BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a BZIP2 output stream.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link BZip2CompressorOutputStream#MAX_BLOCKSIZE}.
     * 
     * @return The compression level to use when writing a BZIP2 output stream.
     */
    public int getLevel() {
        return BZip2CompressorOutputStream.MAX_BLOCKSIZE;
    }

    @Override
    protected InputService<TarDriverEntry> newInputService(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final class Source extends AbstractSource {
            @Override
            public InputStream stream() throws IOException {
                final InputStream in = input.stream();
                try {
                    return new BZip2CompressorInputStream(
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
    protected OutputService<TarDriverEntry> newOutputService(
            final FsModel model,
            final OutputSocket<?> output,
            final InputService<TarDriverEntry> source)
    throws IOException {
        final class Sink extends AbstractSink {
            @Override
            public OutputStream stream() throws IOException {
                final OutputStream out = output.stream();
                try {
                    return new BZip2CompressorOutputStream(
                            new BufferedOutputStream(out, getBufferSize()),
                            getLevel());
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

        return new MultiplexingOutputService<>(
                new TarOutputService(model, new Sink(), this),
                getIOPool());
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected OutputSocket<?> getOutputSocket(
            FsController<?> controller,
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return controller.getOutputSocket(name, options.set(STORE), null);
    }

    private static final class BZip2CompressorOutputStream
    extends org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream {
        final OutputStream out;

        BZip2CompressorOutputStream(final OutputStream out, final int level)
        throws IOException {
            super(out, level);
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            super.close();
            // Workaround for super class implementation which may not have
            // been left in a consistent state if the decorated stream has
            // thrown an IOException upon the first call to its close() method.
            // See http://java.net/jira/browse/TRUEZIP-234
            out.close();
        }
    } // BZip2CompressorOutputStream
}
