/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.zip;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.spi.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.archive.spi.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.spi.InputArchive;
import de.schlichtherle.truezip.io.archive.spi.MultiplexedOutputArchive;
import de.schlichtherle.truezip.io.archive.spi.OutputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import javax.swing.Icon;

/**
 * An archive driver which builds ZIP files.
 * Note that this driver does not check the CRC value of any entries in
 * existing archives.
 * <p>
 * Instances of this class are immutable.
 *
 * @see CheckedZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class ZipDriver extends AbstractArchiveDriver {
    private static final long serialVersionUID = -7061546656075796996L;

    /** Prefix for temporary files created by this driver. */
    static final String TEMP_FILE_PREFIX = "tzp-zip";

    /**
     * The default character set to use for entry names and comments,
     * which is {@value}.
     */
    public static final String DEFAULT_CHARSET = "IBM437";

    /**
     * The default compression level to use when writing a ZIP output stream,
     * which is {@value}.
     */
    public static final int DEFAULT_LEVEL = Deflater.BEST_COMPRESSION;

    private final boolean preambled, postambled;
    private final int level;

    /**
     * Equivalent to {@link #ZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public ZipDriver() {
        this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(charset, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public ZipDriver(String charset) {
        this(charset, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, false, level)}.
     */
    public ZipDriver(int level) {
        this(DEFAULT_CHARSET, null, null, false, false, level);
    }

    public ZipDriver(
            final String charset,
            final boolean preambled,
            final boolean postambled,
            final Icon openIcon,
            final Icon closedIcon) {
        this(charset, openIcon, closedIcon, preambled, postambled, DEFAULT_LEVEL);
    }

    /**
     * Constructs a new ZIP driver.
     *
     * @param preambled {@code true} if and only if a prospective ZIP
     *        compatible file is allowed to contain preamble data before the
     *        actual ZIP file data.
     *        Self Extracting Archives typically use the preamble to store the
     *        application code that is required to extract the ZIP file contents.
     *        <p>
     *        Please note that any ZIP compatible file may actually have a
     *        preamble. However, for performance reasons this parameter should
     *        be set to {@code false}, unless required.
     * @param postambled {@code true} if and only if a prospective ZIP
     *        compatible file is allowed to have a postamble of arbitrary
     *        length.
     *        If set to {@code false}, the ZIP compatible file may still
     *        have a postamble. However, the postamble must not exceed 64KB
     *        size, including the End Of Central Directory record and thus
     *        the ZIP file comment. This causes the initial ZIP file
     *        compatibility test to fail fast if the file is not compatible
     *        to the ZIP File Format Specification.
     *        For performance reasons, this parameter should be set to
     *        {@code false} unless you need to support Self Extracting
     *        Archives with very large postambles.
     * @param level The compression level to use when deflating an entry to
     *        a ZIP output stream.
     * @throws IllegalArgumentException If {@code level} is not in the
     *         range [{@value java.util.zip.Deflater#NO_COMPRESSION}, {@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public ZipDriver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon,
            final boolean preambled,
            final boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon);
        if (    (   level < Deflater.NO_COMPRESSION
                 || level > Deflater.BEST_COMPRESSION)
                && level != Deflater.DEFAULT_COMPRESSION)
            throw new IllegalArgumentException();
        this.preambled = preambled;
        this.postambled = postambled;
        this.level = level;
    }

    //
    // Properties:
    //
    
    /**
     * Returns the value of the property {@code preambled} which was 
     * provided to the constructor.
     */
    public final boolean getPreambled() {
        return preambled;
    }

    /**
     * Returns the value of the property {@code postambled} which was 
     * provided to the constructor.
     */
    public final boolean getPostambled() {
        return postambled;
    }

    /**
     * Returns the value of the property {@code level} which was 
     * provided to the constructor.
     */
    public final int getLevel() {
        return level;
    }

    //
    // Factory methods:
    //

    public ArchiveEntry createArchiveEntry(
            final Archive archive,
            final String entryName,
            final ArchiveEntry template)
    throws CharConversionException {
        ensureEncodable(entryName);

        final ZipEntry entry;
        if (template != null) {
            if (template instanceof ZipEntry) {
                entry = createZipEntry((ZipEntry) template);
                entry.setName(entryName);
            } else {
                entry = createZipEntry(entryName);
                entry.setTime(template.getTime());
                entry.setSize(template.getSize());
            }
        } else {
            entry = createZipEntry(entryName);
        }

        return entry;
    }

    protected ZipEntry createZipEntry(ZipEntry template) {
        return new ZipEntry(template);
    }

    protected ZipEntry createZipEntry(String entryName) {
        return new ZipEntry(entryName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipDriver} simply forwards the call to
     * {@link #createZipInputArchive}.
     */
    public InputArchive createInputArchive(Archive archive, ReadOnlyFile rof)
    throws IOException {
        return createZipInputArchive(archive, rof);
    }

    protected ZipInputArchive createZipInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new ZipInputArchive(
                rof, getCharset(), ZipEntryFactory.INSTANCE,
                getPreambled(), getPostambled());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipDriver} simply forwards the call to
     * {@link #createZipOutputArchive} and wraps the result in a new
     * {@link MultiplexedOutputArchive}.
     */
    public OutputArchive createOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException {
        return new MultiplexedOutputArchive(createZipOutputArchive(
                archive, out, (ZipInputArchive) source));
    }

    protected ZipOutputArchive createZipOutputArchive(
            Archive archive,
            OutputStream out,
            ZipInputArchive source)
    throws IOException {
        return new ZipOutputArchive(out, getCharset(), level, source);
    }
}
