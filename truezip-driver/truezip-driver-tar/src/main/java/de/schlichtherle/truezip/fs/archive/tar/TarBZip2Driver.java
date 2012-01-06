/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * An archive driver which builds BZIP2 compressed TAR files (TAR.BZIP2).
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class TarBZip2Driver extends TarDriver {

    public TarBZip2Driver(IOPoolProvider provider) {
        super(provider);
    }

    public static final int BUFFER_SIZE = 4096;

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

    /**
     * Returns a newly created and verified {@link BZip2CompressorInputStream}.
     * This method performs a simple verification by computing the checksum
     * for the first record only.
     * This method is required because the {@code BZip2CompressorInputStream}
     * unfortunately does not do sufficient verification!
     */
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
}
