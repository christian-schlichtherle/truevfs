/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.zip.ZipEntry;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPoolService;
import de.schlichtherle.truezip.zip.ZipEntryFactory;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
//import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * An archive driver which builds ZIP files.
 * Note that this driver does not check the CRC value of any entries in
 * existing archives.
 *
 * @see CheckedZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class ZipDriver
extends FsCharsetArchiveDriver<ZipArchiveEntry>
implements ZipEntryFactory<ZipArchiveEntry> {

    /**
     * The default character set for entry names and comments, which is
     * {@code "IBM437"}.
     */
    private static final Charset ZIP_CHARSET = Charset.forName("IBM437");

    private final IOPool<?> pool;

    /**
     * Equivalent to
     * {@link ZipDriver#ZipDriver(IOPoolService, Charset) new ZipDriver(pool, ZIP_CHARSET)}.
     */
    public ZipDriver(final IOPoolService service) {
        this(service, ZIP_CHARSET);
    }

    /**
     * Constructs a new ZIP driver.
     *
     * @param service the I/O pool service to use for allocating temporary I/O
     *        entries.
     * @param charset the character set to use for entry names and comments.
     */
    protected ZipDriver(final IOPoolService service, Charset charset) {
        super(charset);
        this.pool = service.get();
    }

    @Override
    public final IOPool<?> getPool() {
        return pool;
    }

    /**
     * Returns the value of the property {@code preambled}.
     * If this is {@code true}, then a ZIP file is allowed to contain arbitrary
     * data as its preamble before the actual ZIP file data.
     * Self Extracting Archives typically use a preamble to store the
     * application code that is required to extract the ZIP file contents.
     * <p>
     * Note that searching for a preamble can seriously degrade the performance
     * if the file is not compatible to the ZIP File Format Specification.
     * <p>
     * The implementation in the class {@link ZipDriver} returns {@code false}.
     *
     * @return The value of the property {@code preambled}.
     */
    public boolean getPreambled() {
        return false;
    }

    /**
     * Returns the value of the property {@code postambled}.
     * If this is {@code true}, then a ZIP file is allowed to contain arbitrary
     * length data as its postamble after the actual ZIP file data.
     * <p>
     * If this is {@code false}, then a ZIP file may still have a postamble.
     * However, the postamble must not exceed 64KB size, including the End Of
     * Central Directory record and thus the ZIP file comment.
     * This causes the initial ZIP file compatibility test to fail fast if the
     * file is not compatible to the ZIP File Format Specification.
     * <p>
     * Note that searching for an arbitrary length postamble can seriously
     * degrade the performance if the file is not compatible to the ZIP File
     * Format Specification.
     * So this should be set to {@code true} only if Self Extracting Archives
     * with very large postambles need to get supported.
     * <p>
     * The implementation in the class {@link ZipDriver} returns {@code false}.
     *
     * @return The value of the property {@code postambled}.
     */
    public boolean getPostambled() {
        return false;
    }

    /**
     * Returns the value of the property {@code method}.
     * This is the compression method to use when writing an entry to a ZIP
     * output stream.
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@link ZipEntry#DEFLATED}.
     *
     * @return The value of the property {@code method}.
     */
    public int getMethod() {
        return ZipEntry.DEFLATED;
    }

    /**
     * Returns the value of the property {@code level}.
     * This is the compression level to use when deflating an entry to a ZIP
     * output stream.
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@link Deflater#BEST_COMPRESSION}.
     *
     * @return The value of the property {@code level}.
     */
    public int getLevel() {
        return Deflater.BEST_COMPRESSION;
    }

    @Override
    public ZipArchiveEntry newEntry(
            String name,
            final Type type,
            final Entry template)
    throws CharConversionException {
        assertEncodable(name);
        name = toZipOrTarEntryName(name, type);
        final ZipArchiveEntry entry;
        if (template instanceof ZipArchiveEntry) {
            entry = newEntry(name, (ZipArchiveEntry) template);
        } else {
            entry = newEntry(name);
            if (null != template) {
                entry.setTime(template.getTime(WRITE));
                entry.setSize(template.getSize(DATA));
            }
        }
        return entry;
    }

    @Override
    public ZipArchiveEntry newEntry(String name) {
        return new ZipArchiveEntry(name);
    }

    public ZipArchiveEntry newEntry(String name, ZipArchiveEntry template) {
        return new ZipArchiveEntry(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newZipInputShop}.
     */
    @Override
    public ZipInputShop newInputShop(FsConcurrentModel model, InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newZipInputShop(model, rof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    protected ZipInputShop newZipInputShop(FsConcurrentModel model, ReadOnlyFile rof)
    throws IOException {
        return new ZipInputShop(this, rof);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipDriver} simply forwards the call to
     * {@link #newZipOutputShop} and wraps the result in a new
     * {@link FsMultiplexedArchiveOutputShop}.
     */
    @Override
    public OutputShop<ZipArchiveEntry> newOutputShop(
            FsConcurrentModel model,
            OutputSocket<?> output,
            @CheckForNull InputShop<ZipArchiveEntry> source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return new FsMultiplexedArchiveOutputShop<ZipArchiveEntry>(
                    newZipOutputShop(model, out, (ZipInputShop) source),
                    getPool());
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    protected ZipOutputShop newZipOutputShop(
            FsConcurrentModel model, OutputStream out, @CheckForNull ZipInputShop source)
    throws IOException {
        return new ZipOutputShop(this, out, source);
    }
}

