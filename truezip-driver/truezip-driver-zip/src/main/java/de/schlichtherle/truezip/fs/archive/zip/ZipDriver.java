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

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.zip.ZipEntry;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import de.schlichtherle.truezip.zip.ZipEntryFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which builds ZIP files.
 * Do <em>not</em> use this driver for custom application file formats
 * - use {@link JarDriver} instead!
 * <p>
 * This driver does <em>not</em> check the CRC value of any entries in existing
 * archives
 * - use {@link CheckedZipDriver} instead!
 *
 * @see     CheckedZipDriver
 * @author  Christian Schlichtherle
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

    private final IOPoolProvider provider;

    /**
     * Equivalent to
     * {@link ZipDriver#ZipDriver(IOPoolProvider, Charset) new ZipDriver(provider, ZIP_CHARSET)}.
     */
    public ZipDriver(final IOPoolProvider provider) {
        this(provider, ZIP_CHARSET);
    }

    /**
     * Constructs a new ZIP driver.
     *
     * @param provider the I/O pool service to use for allocating temporary I/O
     *        entries.
     * @param charset the character set to use for entry names and comments.
     */
    protected ZipDriver(final IOPoolProvider provider, Charset charset) {
        super(charset);
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
    }

    @Override
    protected final IOPool<?> getPool() {
        return provider.get();
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
        return DEFLATED;
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

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        /*if (options.get(GROW))
            options = options.clear(GROW).set(APPEND);*/
        return controller.getOutputSocket(name, options.set(STORE), template);
    }

    @Override
    public ZipArchiveEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsOutputOption> mknod)
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
        if (mknod.get(COMPRESS)) { // #1 priority
            if (DEFLATED != entry.getMethod()) {
                entry.setMethod(DEFLATED);
                entry.setCompressedSize(UNKNOWN);
            }
        } else if (mknod.get(STORE)) { // #2 priority
            entry.setMethod(STORED);
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
    public ZipInputShop newInputShop(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newZipInputShop(model, rof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    protected ZipInputShop newZipInputShop(FsModel model, ReadOnlyFile rof)
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
            final FsModel model,
            final OutputSocket<?> output,
            final @CheckForNull InputShop<ZipArchiveEntry> source)
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
            FsModel model, OutputStream out, @CheckForNull ZipInputShop source)
    throws IOException {
        return new ZipOutputShop(this, out, source);
    }
}
