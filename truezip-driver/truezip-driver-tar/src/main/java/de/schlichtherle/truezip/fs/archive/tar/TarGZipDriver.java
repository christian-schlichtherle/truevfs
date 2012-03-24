/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.addr.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import static de.schlichtherle.truezip.fs.option.FsOutputOption.STORE;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.cio.IOPoolProvider;
import de.schlichtherle.truezip.cio.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import javax.annotation.CheckForNull;
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

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options.set(STORE), template);
    }

    @Override
    protected TarInputService newTarInputService(FsModel model, InputStream in)
    throws IOException {
        return super.newTarInputService(model,
                new GZIPInputStream(in, getBufferSize()));
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

    /** Extends its super class to set the deflater level. */
    private static final class GZIPOutputStream
    extends java.util.zip.GZIPOutputStream {
        final OutputStream delegate;

        GZIPOutputStream(OutputStream out, int size, int level)
        throws IOException {
            super(out, size);
            def.setLevel(level);
            this.delegate = out;
        }

        @Override
        public void close() throws IOException {
            super.close();
            // Workaround for super class implementation which may not have
            // been left in a consistent state if the decorated stream has
            // thrown an IOException upon the first call to its close() method.
            // See http://java.net/jira/browse/TRUEZIP-234
            delegate.close();
        }
    } // GZIPOutputStream
}
