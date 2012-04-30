/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.STORE;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.*;
import javax.annotation.CheckForNull;
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

    public TarBZip2Driver(IOPoolProvider provider) {
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
    protected TarInputShop newTarInputShop(
            final FsModel model, final InputStream in)
    throws IOException {
        return super.newTarInputShop(model,
                new BZip2CompressorInputStream(
                    new BufferedInputStream(in, getBufferSize())));
    }

    @Override
    protected TarOutputShop newTarOutputShop(
            final FsModel model,
            final OutputStream out,
            final TarInputShop source)
    throws IOException {
        return super.newTarOutputShop(model,
                new BZip2CompressorOutputStream(
                    new BufferedOutputStream(out, getBufferSize()),
                    getLevel()),
                source);
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
            // Workaround for super class implementation which fails to close
            // the decorated stream on a subsequent call if the initial attempt
            // failed with an IOException.
            // See http://java.net/jira/browse/TRUEZIP-234
            super.close();
            out.close();
        }
    } // BZip2CompressorOutputStream
}
