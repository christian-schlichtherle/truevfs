/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.FsConcurrentModel;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * An archive driver which builds TAR files compressed with BZIP2.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class TarBZip2Driver extends TarDriver {

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
     * {@link CBZip2OutputStream#MAX_BLOCKSIZE}.
     */
    public int getLevel() {
        return CBZip2OutputStream.MAX_BLOCKSIZE;
    }

    /**
     * Returns a newly created and verified {@link CBZip2InputStream}.
     * This method performs a simple verification by computing the checksum
     * for the first record only.
     * This method is required because the {@code CBZip2InputStream}
     * unfortunately does not do sufficient verification!
     */
    @Override
    protected TarInputShop newTarInputShop(
            final FsConcurrentModel model, final InputStream in)
    throws IOException {
        // Consume and check the first two magic bytes. This is required for
        // the CBZip2InputStream class.
        if (in.read() != 'B' || in.read() != 'Z')
            throw new IOException("Not a BZIP2 compressed input stream!");
        final byte[] magic = new byte[2];
        final InputStream vin = TarInputShop.readAhead(in, magic);
        if (magic[0] != 'h' || magic[1] < '1' || '9' < magic[1])
            throw new IOException("Not a BZIP2 compressed input stream!");
        return new TarInputShop(
                new CBZip2InputStream(
                    new BufferedInputStream(vin, getBufferSize())));
    }

    @Override
    protected TarOutputShop newTarOutputShop(
            final FsConcurrentModel model,
            final OutputStream out,
            final TarInputShop source)
    throws IOException {
        // Produce the first two magic bytes. This is required for the
        // CBZip2OutputStream class.
        out.write(new byte[] { 'B', 'Z' });
        return super.newTarOutputShop(
                model,
                new CBZip2OutputStream(
                    new BufferedOutputStream(out, getBufferSize()),
                    getLevel()),
                source);
    }
}
