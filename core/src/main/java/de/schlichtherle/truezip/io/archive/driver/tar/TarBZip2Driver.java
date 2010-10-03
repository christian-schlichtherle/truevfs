/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * An archive driver which builds TAR files compressed with BZIP2.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarBZip2Driver extends TarDriver {

    private static final long serialVersionUID = 4966248471134003932L;

    private static final int BUFSIZE = 4096;

    /**
     * The minimum block size to use when writing a BZIP2 output stream,
     * which is {@value}.
     */
    public static final int MIN_BLOCKSIZE = 1;

    /**
     * The maximum block size to use when writing a BZIP2 output stream,
     * which is {@value}.
     */
    public static final int MAX_BLOCKSIZE = 9;

    /**
     * The default block size to use when writing a BZIP2 output stream,
     * which is {@value}.
     */
    public static final int DEFAULT_BLOCKSIZE = MAX_BLOCKSIZE;

    private final int inBlockSize;

    /**
     * Equivalent to {@link #TarBZip2Driver(String, int)
     * this(TAR_CHARSET, DEFAULT_BLOCKSIZE)}.
     */
    public TarBZip2Driver() {
        this(TAR_CHARSET, DEFAULT_BLOCKSIZE);
    }

    /**
     * Equivalent to {@link #TarBZip2Driver(String, int)
     * this(charset, DEFAULT_BLOCKSIZE)}.
     */
    public TarBZip2Driver(String charset) {
        this(charset, DEFAULT_BLOCKSIZE);
    }

    /**
     * Equivalent to {@link #TarBZip2Driver(String, int)
     * this(TAR_CHARSET, inBlockSize)}.
     */
    public TarBZip2Driver(int inBlockSize) {
        this(TAR_CHARSET,  inBlockSize);
    }

    /**
     * Constructs a new TAR.BZ2 driver.
     *
     * @param inBlockSize The compression block size to use when writing
     *        a BZIP2 output stream.
     * @throws IllegalArgumentException If {@code inBlockSize} is not
     *         in the range [1..9].
     */
    public TarBZip2Driver(final String charset, final int inBlockSize) {
        super(charset);
        if (inBlockSize < MIN_BLOCKSIZE || MAX_BLOCKSIZE < inBlockSize)
            throw new IllegalArgumentException(inBlockSize + "");
        this.inBlockSize = inBlockSize;
    }

    /**
     * Returns the value of the property {@code inBlockSize} which was
     * provided to the constructor.
     */
    public final int getLevel() {
        return inBlockSize;
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
            final ArchiveDescriptor archive,
            final InputStream in)
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
                    new BufferedInputStream(vin, BUFSIZE)));
    }

    @Override
    protected TarOutputShop newTarOutputShop(
            final ArchiveDescriptor archive,
            final OutputStream out,
            final TarInputShop source)
    throws IOException {
        // Produce the first two magic bytes. This is required for the
        // CBZip2OutputStream class.
        out.write(new byte[] { 'B', 'Z' });
        return super.newTarOutputShop(
                archive,
                new CBZip2OutputStream(
                    new BufferedOutputStream(out, BUFSIZE),
                    inBlockSize),
                source);
    }
}
