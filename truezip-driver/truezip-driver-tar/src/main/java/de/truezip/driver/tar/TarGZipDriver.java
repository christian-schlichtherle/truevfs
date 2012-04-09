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
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.io.AbstractSource;
import de.truezip.kernel.io.Source;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for GZIP compressed TAR files (TAR.GZIP).
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class TarGZipDriver extends TarDriver {

    public TarGZipDriver(IOPoolProvider provider) {
        super(provider);
    }

    /**
     * The buffer size used for reading and writing.
     * Optimized for performance.
     */
    public static final int BUFFER_SIZE = Streams.BUFFER_SIZE;

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarGZipDriver} returns
     * {@link #BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a GZIP output stream.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link Deflater#BEST_COMPRESSION}.
     * 
     * @return The compression level to use when writing a GZIP output stream.
     */
    public int getLevel() {
        return Deflater.BEST_COMPRESSION;
    }

    @Override
    protected TarInputService newTarInputService(
            final FsModel model,
            final Source source)
    throws IOException {
        final class GZipSource extends AbstractSource {
            @Override
            public InputStream stream() throws IOException {
                final InputStream in = source.stream();
                try {
                    return new GZIPInputStream(in, getBufferSize());
                } catch(final Throwable ex) {
                    try {
                        in.close();
                    } catch (final Throwable ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
        } // GZipSource

        return new TarInputService(this, model, new GZipSource());
    }

    @Override
    protected TarOutputService newTarOutputService(
            final FsModel model,
            final OutputStream out,
            final TarInputService source)
    throws IOException {
        return super.newTarOutputService(model,
                new GZIPOutputStream(out, getBufferSize(), getLevel()),
                source);
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

    /** Extends its super class to set the deflater level. */
    private static final class GZIPOutputStream
    extends java.util.zip.GZIPOutputStream {
        final OutputStream out;

        GZIPOutputStream(OutputStream out, int size, int level)
        throws IOException {
            super(out, size);
            def.setLevel(level);
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
    } // GZIPOutputStream
}
