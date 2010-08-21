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

package de.schlichtherle.truezip.io.archive.tar;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.spi.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.archive.spi.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.spi.InputArchive;
import de.schlichtherle.truezip.io.archive.spi.MultiplexedOutputArchive;
import de.schlichtherle.truezip.io.archive.spi.OutputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.Icon;

/**
 * An archive driver which builds TAR files.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class TarDriver extends AbstractArchiveDriver {

    private static final long serialVersionUID = 6622746562629104174L;

    /** Prefix for temporary files created by this driver. */
    static final String TEMP_FILE_PREFIX = "tzp-tar";

    /**
     * The character set to use for entry names, which is {@value}.
     * TAR files should actually be able to use the system's native character
     * set charset. However, the low level TAR code as of Ant 1.6.5 doesn't
     * support that, hence this constraint.
     */
    public static final String DEFAULT_CHARSET = "US-ASCII";

    /**
     * Equivalent to {@link #TarDriver(String, Icon, Icon)
     * this(DEFAULT_CHARSET, null, null)}.
     */
    public TarDriver() {
        this(DEFAULT_CHARSET, null, null);
    }

    /**
     * Equivalent to {@link #TarDriver(String, Icon, Icon)
     * this(charset, null, null)}.
     */
    public TarDriver(String charset) {
        this(charset, null, null);
    }

    /**
     * Constructs a new TAR driver.
     *
     * @param charset The name of a character set to use for all entry names
     *        when reading or writing TAR files.
     *        <b>Warning:</b> Due to limitations in Apache's Ant code, using
     *        anything else than {@value #DEFAULT_CHARSET} is currently not
     *        supported!
     */
    public TarDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon) {
        super(charset, openIcon, closedIcon);
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

        final TarEntry entry;
        if (template != null) {
            if (template instanceof TarEntry) {
                entry = new TarEntry((TarEntry) template);
                entry.setName(entryName);
            } else {
                entry = new TarEntry(entryName);
                entry.setTime(template.getTime());
                entry.setSize(template.getSize());
            }
        } else {
            entry = new TarEntry(entryName);
        }

        return entry;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #createInputStream(Archive, ReadOnlyFile)
     * createInputStream(archive, rof)} and passes the resulting stream to
     * {@link #createTarInputArchive(Archive, InputStream)}.
     */
    public InputArchive createInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        final InputStream in = createInputStream(archive, rof);
        try {
            return createTarInputArchive(archive, in);
        } finally {
            in.close();
        }
    }

    /**
     * Returns a new {@code InputStream} to read the contents from the
     * given {@code ReadOnlyFile} from.
     * Override this method in order to decorate the stream returned by the
     * implementation in this class in order to have the driver read the TAR
     * file from wrapper file formats such as GZIP or BZIP2.
     * <p>
     * Note that the returned stream should support marking for best
     * performance and will <em>always</em> be closed early by
     * {@link #createInputArchive(Archive, ReadOnlyFile)}.
     */
    protected InputStream createInputStream(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new ReadOnlyFileInputStream(rof);
    }

    /**
     * Returns a new {@code TarInputArchive} to read the contents from
     * the given {@code InputStream}.
     * The implementation in this class simply returns
     * {@code new TarInputArchive(in)}.
     */
    protected TarInputArchive createTarInputArchive(
            Archive archive,
            InputStream in)
    throws IOException {
        return new TarInputArchive(in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to {@link #createTarOutputArchive}
     * and wraps the result in a new {@link MultiplexedOutputArchive}.
     */
    public OutputArchive createOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException {
        return new MultiplexedOutputArchive(createTarOutputArchive(
                archive, out, (TarInputArchive) source));
    }

    protected TarOutputArchive createTarOutputArchive(
            Archive archive,
            OutputStream out,
            TarInputArchive source)
    throws IOException {
        return new TarOutputArchive(out);
    }
}
