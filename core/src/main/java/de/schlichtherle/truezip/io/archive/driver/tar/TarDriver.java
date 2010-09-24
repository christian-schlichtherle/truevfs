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
import de.schlichtherle.truezip.io.archive.driver.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.common.CommonEntry;
import de.schlichtherle.truezip.io.socket.common.CommonEntry.Type;
import de.schlichtherle.truezip.io.archive.output.MultiplexedArchiveOutput;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
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
public class TarDriver
extends AbstractArchiveDriver<TarEntry, TarInput, ArchiveOutput<TarEntry>> {

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
    public TarEntry newEntry(
            String path,
            final Type type,
            final CommonEntry template)
    throws CharConversionException {
        path = toZipOrTarEntryName(path, type);
        final TarEntry entry;
        if (template != null) {
            if (template instanceof TarEntry) {
                entry = newEntry((TarEntry) template);
                entry.setName(path);
            } else {
                entry = newEntry(path);
                entry.setTime(template.getTime());
                entry.setSize(template.getSize());
            }
        } else {
            entry = newEntry(path);
        }

        return entry;
    }

    public TarEntry newEntry(String name) {
        return new TarEntry(name);
    }

    public TarEntry newEntry(TarEntry template) {
        return new TarEntry(template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #newInputStream(ArchiveDescriptor, ReadOnlyFile)
     * newInputStream(archive, rof)} and passes the resulting stream to
     * {@link #newTarInput(ArchiveDescriptor, InputStream)}.
     */
    @Override
    public TarInput newInput(
            ArchiveDescriptor archive,
            ReadOnlyFile rof)
    throws IOException {
        final InputStream in = newInputStream(archive, rof);
        try {
            return newTarInput(archive, in);
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
     * {@link #newInput(ArchiveDescriptor, ReadOnlyFile)}.
     */
    protected InputStream newInputStream(
            ArchiveDescriptor archive,
            ReadOnlyFile rof)
    throws IOException {
        return new ReadOnlyFileInputStream(rof);
    }

    /**
     * Returns a new {@code TarInput} to read the contents from
     * the given {@code InputStream}.
     * The implementation in this class simply returns
     * {@code new TarInput(in)}.
     */
    protected TarInput newTarInput(
            ArchiveDescriptor archive,
            InputStream in)
    throws IOException {
        return new TarInput(in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to {@link #newTarOutput}
     * and wraps the result in a new {@link MultiplexedArchiveOutput}.
     */
    @Override
    public ArchiveOutput<TarEntry> newOutput(
            ArchiveDescriptor archive,
            OutputStream out,
            TarInput source)
    throws IOException {
        return new MultiplexedArchiveOutput(newTarOutput(
                archive, out, (TarInput) source));
    }

    protected TarOutput newTarOutput(
            ArchiveDescriptor archive,
            OutputStream out,
            TarInput source)
    throws IOException {
        return new TarOutput(out);
    }
}
