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

package de.schlichtherle.truezip.io.archive.driver.impl.tar;

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.spi.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.archive.driver.spi.MultiplexedOutputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An archive driver which builds TAR files.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
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
    public static final String TAR_CHARSET = "US-ASCII";

    /**
     * Equivalent to {@link #TarDriver(String)
     * this(TAR_CHARSET)}.
     */
    public TarDriver() {
        this(TAR_CHARSET);
    }

    /**
     * Constructs a new TAR driver.
     *
     * @param charset The name of a character set to use for all entry names
     *        when reading or writing TAR files.
     *        <b>Warning:</b> Due to limitations in Apache's Ant code, using
     *        anything else than {@value #TAR_CHARSET} is currently not
     *        supported!
     */
    public TarDriver(String charset) {
        super(charset);
    }

    //
    // Factory methods:
    //

    @Override
    public TarEntry newArchiveEntry(
            String path,
            final Type type,
            final ArchiveEntry template)
    throws CharConversionException {
        path = toZipOrTarEntryName(path, type);
        final TarEntry entry;
        if (template != null) {
            if (template instanceof TarEntry) {
                entry = newTarEntry((TarEntry) template);
                entry.setName(path);
            } else {
                entry = newTarEntry(path);
                entry.setTime(template.getTime());
                entry.setSize(template.getSize());
            }
        } else {
            entry = newTarEntry(path);
        }

        return entry;
    }

    public TarEntry newTarEntry(String name) {
        return new TarEntry(name);
    }

    public TarEntry newTarEntry(TarEntry template) {
        return new TarEntry(template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #newInputStream(ArchiveDescriptor, ReadOnlyFile)
     * newInputStream(archive, rof)} and passes the resulting stream to
     * {@link #newTarInputArchive(ArchiveDescriptor, InputStream)}.
     */
    public InputArchive newInputArchive(
            ArchiveDescriptor archive,
            ReadOnlyFile rof)
    throws IOException {
        final InputStream in = newInputStream(archive, rof);
        try {
            return newTarInputArchive(archive, in);
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
     * {@link #newInputArchive(ArchiveDescriptor, ReadOnlyFile)}.
     */
    protected InputStream newInputStream(
            ArchiveDescriptor archive,
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
    protected TarInputArchive newTarInputArchive(
            ArchiveDescriptor archive,
            InputStream in)
    throws IOException {
        return new TarInputArchive(in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to {@link #newTarOutputArchive}
     * and wraps the result in a new {@link MultiplexedOutputArchive}.
     */
    public OutputArchive newOutputArchive(
            ArchiveDescriptor archive,
            OutputStream out,
            InputArchive source)
    throws IOException {
        return new MultiplexedOutputArchive(newTarOutputArchive(
                archive, out, (TarInputArchive) source));
    }

    protected TarOutputArchive newTarOutputArchive(
            ArchiveDescriptor archive,
            OutputStream out,
            TarInputArchive source)
    throws IOException {
        return new TarOutputArchive(out);
    }
}
