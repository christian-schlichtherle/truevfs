/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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
import de.schlichtherle.io.archive.spi.*;
import de.schlichtherle.io.rof.*;

import java.io.*;
import java.util.zip.*;

import javax.swing.*;

/**
 * An archive driver which builds TAR files compressed with GZIP.
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class TarGZipDriver extends TarDriver {

    private static final long serialVersionUID = 7736164529936091928L;

    private static final int BUFSIZE = 4096;

    /**
     * The default compression level to use when writing a GZIP output stream,
     * which is {@value}.
     */
    public static final int DEFAULT_LEVEL = Deflater.BEST_COMPRESSION;

    private final int level;

    /**
     * Equivalent to {@link #TarGZipDriver(String, Icon, Icon, int)
     * this(DEFAULT_CHARSET, null, null, DEFAULT_LEVEL)}.
     */
    public TarGZipDriver() {
        this(DEFAULT_CHARSET, null, null, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #TarGZipDriver(String, Icon, Icon, int)
     * this(charset, null, null, DEFAULT_LEVEL)}.
     */
    public TarGZipDriver(String charset) {
        this(charset, null, null, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #TarGZipDriver(String, Icon, Icon, int)
     * this(DEFAULT_CHARSET, null, null, level)}.
     */
    public TarGZipDriver(int level) {
        this(DEFAULT_CHARSET, null, null, level);
    }

    /**
     * Constructs a new TAR.GZ driver.
     *
     * @param level The compression level to use when writing a GZIP output
     *        stream.
     * @throws IllegalArgumentException If <code>level</code> is not in the
     *         range [{@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public TarGZipDriver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon,
            final int level) {
        super(charset, null, null);
        if (    (   level < Deflater.BEST_SPEED
                 || level > Deflater.BEST_COMPRESSION)
                && level != Deflater.DEFAULT_COMPRESSION)
            throw new IllegalArgumentException();
        this.level = level;
    }

    /**
     * Returns the value of the property <code>preambled</code> which was 
     * provided to the constructor.
     */
    public final int getLevel() {
        return level;
    }

    //
    // Driver implementation:
    //

    protected InputStream createInputStream(Archive archive, ReadOnlyFile rof)
    throws IOException {
        return new GZIPInputStream(
                super.createInputStream(archive, rof),
                BUFSIZE);
    }

    protected TarOutputArchive createTarOutputArchive(
            final Archive archive,
            final OutputStream out,
            final TarInputArchive source)
    throws IOException {
        return super.createTarOutputArchive(
                archive,
                new GZIPOutputStream(out, BUFSIZE, level),
                source);
    }

    public static class GZIPOutputStream
            extends java.util.zip.GZIPOutputStream {
        /**
         * Constructs a new <code>GZIPOutputStream</code> with the specified
         * output stream, buffer size and compression level.
         *
         * @param level The compression level for the {@link Deflater}
         *        ({@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}).
         * @throws IOException If an I/O error occurs.
         * @throws IllegalArgumentException if <code>size</code> is <= 0.
         * @throws IllegalArgumentException If <code>level</code> is not in the
         *         range [{@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}]
         *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
         */
        public GZIPOutputStream(OutputStream out, int size, int level)
        throws IOException {
            super(out, size);
            def.setLevel(level);
        }
    }
}
