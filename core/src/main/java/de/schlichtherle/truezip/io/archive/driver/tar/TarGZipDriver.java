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

package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import static java.util.zip.Deflater.NO_COMPRESSION;

/**
 * An archive driver which builds TAR files compressed with GZIP.
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarGZipDriver extends TarDriver {

    private static final long serialVersionUID = 7736164529936091928L;

    private static final int BUFSIZE = 4096;

    private final int level;

    /**
     * Equivalent to {@link #TarGZipDriver(String, int)
     * this(TAR_CHARSET, Deflater.BEST_COMPRESSION)}.
     */
    public TarGZipDriver() {
        this(TAR_CHARSET, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #TarGZipDriver(String, int)
     * this(charset, Deflater.BEST_COMPRESSION)}.
     */
    public TarGZipDriver(String charset) {
        this(charset, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #TarGZipDriver(String, int)
     * this(TAR_CHARSET, level)}.
     */
    public TarGZipDriver(int level) {
        this(TAR_CHARSET, level);
    }

    /**
     * Constructs a new TAR.GZ driver.
     *
     * @param level The compression level to use when writing a GZIP output
     *        stream.
     * @throws IllegalArgumentException If {@code level} is not in the
     *         range [{@value java.util.zip.Deflater#NO_COMPRESSION},
     *         {@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public TarGZipDriver(
            final String charset,
            final int level) {
        super(charset);
        if (    (   level < NO_COMPRESSION
                 || level > BEST_COMPRESSION)
                && level != DEFAULT_COMPRESSION)
            throw new IllegalArgumentException();
        this.level = level;
    }

    /**
     * Returns the value of the property {@code preambled} which was 
     * provided to the constructor.
     */
    public final int getLevel() {
        return level;
    }

    //
    // Driver implementation:
    //

    @Override
    protected InputStream newInputStream(ArchiveDescriptor archive, ReadOnlyFile rof)
    throws IOException {
        return new GZIPInputStream(
                super.newInputStream(archive, rof),
                BUFSIZE);
    }

    @Override
    protected TarOutput newTarOutput(
            final ArchiveDescriptor archive,
            final OutputStream out,
            final TarInput source)
    throws IOException {
        return super.newTarOutput(
                archive,
                new GZIPOutputStream(out, BUFSIZE, level),
                source);
    }

    public static class GZIPOutputStream
            extends java.util.zip.GZIPOutputStream {
        /**
         * Constructs a new {@code GZIPOutputStream} with the specified
         * output stream, buffer size and compression level.
         *
         * @param level The compression level for the {@link Deflater}
         *        ({@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}).
         * @throws IOException If an I/O error occurs.
         * @throws IllegalArgumentException if {@code size} is <= 0.
         * @throws IllegalArgumentException If {@code level} is not in the
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
