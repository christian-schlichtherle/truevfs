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

package de.schlichtherle.io.archive.tar;

import de.schlichtherle.io.archive.*;
import de.schlichtherle.io.rof.*;

import java.io.*;

import javax.swing.*;

import org.apache.tools.bzip2.*;

/**
 * An archive driver which builds TAR files compressed with BZIP2.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
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

    /**
     * @deprecated Use {@link #DEFAULT_BLOCKSIZE} instead.
     */
    public static final int DEFAULT_LEVEL = MAX_BLOCKSIZE;

    private final int inBlockSize;

    /**
     * Equivalent to {@link #TarBZip2Driver(String, Icon, Icon, int)
     * this(DEFAULT_CHARSET, null, null, DEFAULT_BLOCKSIZE)}.
     */
    public TarBZip2Driver() {
        this(DEFAULT_CHARSET, null, null, DEFAULT_BLOCKSIZE);
    }

    /**
     * Equivalent to {@link #TarBZip2Driver(String, Icon, Icon, int)
     * this(charset, null, null, DEFAULT_BLOCKSIZE)}.
     */
    public TarBZip2Driver(String charset) {
        this(charset, null, null, DEFAULT_BLOCKSIZE);
    }

    /**
     * Equivalent to {@link #TarBZip2Driver(String, Icon, Icon, int)
     * this(DEFAULT_CHARSET, null, null, inBlockSize)}.
     */
    public TarBZip2Driver(int inBlockSize) {
        this(DEFAULT_CHARSET, null, null,  inBlockSize);
    }

    /**
     * Equivalent to {@link #TarBZip2Driver(String, Icon, Icon, int)
     * this(charset, null, null, inBlockSize)}.
     */
    public TarBZip2Driver(String charset, int inBlockSize) {
        this(charset, null, null, inBlockSize);
    }

    /**
     * Constructs a new TAR.BZ2 driver.
     *
     * @param inBlockSize The compression block size to use when writing
     *        a BZIP2 output stream.
     * @throws IllegalArgumentException If <code>inBlockSize</code> is not
     *         in the range [1..9].
     */
    public TarBZip2Driver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon,
            final int inBlockSize) {
        super(charset, openIcon, closedIcon);
        if (inBlockSize < MIN_BLOCKSIZE || MAX_BLOCKSIZE < inBlockSize)
            throw new IllegalArgumentException(inBlockSize + "");
        this.inBlockSize = inBlockSize;
    }

    /**
     * Returns the value of the property <code>inBlockSize</code> which was
     * provided to the constructor.
     */
    public final int getLevel() {
        return inBlockSize;
    }

    //
    // Driver implementation:
    //

    /**
     * Returns a newly created and verified {@link CBZip2InputStream}.
     * This method performs a simple verification by computing the checksum
     * for the first record only.
     * This method is required because the <code>CBZip2InputStream</code>
     * unfortunately does not do sufficient verification!
     */
    protected InputStream createInputStream(Archive archive, ReadOnlyFile rof)
    throws IOException {
        final InputStream in = super.createInputStream(archive, rof);
        // Consume and check the first two magic bytes. This is required for
        // the CBZip2InputStream class. Bad design, I think...
        if (in.read() != 'B' || in.read() != 'Z')
            throw new IOException("Not a BZIP2 compressed input stream!");
        final byte[] magic = new byte[2];
        final InputStream vin = TarInputArchive.readAhead(in, magic);
        if (magic[0] != 'h' || magic[1] < '1' || '9' < magic[1])
            throw new IOException("Not a BZIP2 compressed input stream!");
        return new CBZip2InputStream(new BufferedInputStream(vin, BUFSIZE));
    }

    protected TarOutputArchive createTarOutputArchive(
            final Archive archive,
            final OutputStream out,
            final TarInputArchive source)
    throws IOException {
        // Produce the first two magic bytes. This is required for the
        // CBZip2OutputStream class.
        out.write(new byte[] { 'B', 'Z' });
        return super.createTarOutputArchive(
                archive,
                new CBZip2OutputStream(
                    new BufferedOutputStream(out, BUFSIZE),
                    inBlockSize),
                source);
    }
}
