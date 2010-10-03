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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputShop;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.archive.output.MultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.io.socket.output.CommonOutputShop;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.zip.ZipEntryFactory;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import static java.util.zip.Deflater.NO_COMPRESSION;

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
 */
public class ZipDriver
extends AbstractArchiveDriver<ZipEntry>
implements ZipEntryFactory<ZipEntry> {

    private static final long serialVersionUID = -7061546656075796996L;

    /** Prefix for temporary files created by this driver. */
    static final String TEMP_FILE_PREFIX = "tzp-zip";

    /**
     * The default character set to use for entry names and comments,
     * which is {@value}.
     */
    public static final String ZIP_CHARSET = "IBM437";

    private final boolean preambled, postambled;
    private final int level;

    /**
     * Equivalent to {@link #ZipDriver(String, boolean, boolean, int)
     * this(ZIP_CHARSET, null, null, false, false, Deflater.BEST_COMPRESSION)}.
     */
    public ZipDriver() {
        this(ZIP_CHARSET, false, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ZipDriver(String, boolean, boolean, int)
     * this(charset, null, null, false, false, Deflater.BEST_COMPRESSION)}.
     */
    public ZipDriver(String charset) {
        this(charset, false, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ZipDriver(String, boolean, boolean, int)
     * this(ZIP_CHARSET, null, null, false, false, level)}.
     */
    public ZipDriver(int level) {
        this(ZIP_CHARSET, false, false, level);
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
     *         range [{@value java.util.zip.Deflater#NO_COMPRESSION},
     *         {@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public ZipDriver(
            final String charset,
            final boolean preambled,
            final boolean postambled,
            final int level) {
        super(charset);
        if (    (   level < NO_COMPRESSION
                 || level > BEST_COMPRESSION)
                && level != DEFAULT_COMPRESSION)
            throw new IllegalArgumentException();
        this.preambled = preambled;
        this.postambled = postambled;
        this.level = level;
    }
    
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

    @Override
    public ZipEntry newEntry(
            String name,
            final Type type,
            final CommonEntry template)
    throws CharConversionException {
        name = toZipOrTarEntryName(name, type);
        final ZipEntry entry;
        if (template != null) {
            if (template instanceof ZipEntry) {
                entry = newEntry((ZipEntry) template);
                entry.setName(name);
            } else {
                entry = newEntry(name);
                entry.setTime(template.getTime(Access.WRITE));
                entry.setSize(template.getSize());
            }
        } else {
            entry = newEntry(name);
        }
        return entry;
    }

    @Override
    public ZipEntry newEntry(String name) {
        return new ZipEntry(name);
    }

    public ZipEntry newEntry(ZipEntry template) {
        return new ZipEntry(template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newZipInputShop}.
     */
    @Override
    public ZipInputShop newInputShop(
            ArchiveDescriptor archive,
            CommonInputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newZipInputShop(archive, rof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    protected ZipInputShop newZipInputShop(
            ArchiveDescriptor archive,
            ReadOnlyFile rof)
    throws IOException {
        return new ZipInputShop(
                rof, getCharset(), getPreambled(), getPostambled(), this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipDriver} simply forwards the call to
     * {@link #newZipOutputShop} and wraps the result in a new
     * {@link MultiplexedArchiveOutputShop}.
     */
    @Override
    public CommonOutputShop<ZipEntry> newOutputShop(
            ArchiveDescriptor archive, CommonOutputSocket<?> output, CommonInputShop<ZipEntry> source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return new MultiplexedArchiveOutputShop<ZipEntry>(
                    newZipOutputShop(archive, out, (ZipInputShop) source));
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    protected ZipOutputShop newZipOutputShop(
            ArchiveDescriptor archive, OutputStream out, ZipInputShop source)
    throws IOException {
        return new ZipOutputShop(out, getCharset(), level, source);
    }
}
