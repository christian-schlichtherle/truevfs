/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import de.schlichtherle.truezip.zip.ZipEntry;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import de.schlichtherle.truezip.zip.ZipEntryFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
     * The character set for entry names and comments in &quot;traditional&quot;
     * ZIP files, which is {@code "IBM437"}.
     */
    private static final Charset ZIP_CHARSET = Charset.forName("IBM437");

    private final IOPool<?> ioPool;

    /**
     * Constructs a new ZIP file driver.
     * This constructor uses {@link #ZIP_CHARSET} for encoding entry names
     * and comments.
     *
     * @param ioPoolProvider the provider for I/O entry pools for allocating
     *        temporary I/O entries (buffers).
     */
    public ZipDriver(IOPoolProvider ioPoolProvider) {
        this(ioPoolProvider, ZIP_CHARSET);
    }

    /**
     * Constructs a new ZIP file driver.
     *
     * @param ioPoolProvider the provider for I/O entry pools for allocating
     *        temporary I/O entries (buffers).
     * @param charset the character set for encoding entry names and comments.
     */
    protected ZipDriver(IOPoolProvider ioPoolProvider, Charset charset) {
        super(charset);
        this.ioPool = ioPoolProvider.get();
    }

    /**
     * Returns the provider for key managers for accessing protected resources
     * (encryption).
     * <p>
     * The implementation in {@link ZipDriver} always returns
     * {@link KeyManagerLocator#SINGLETON}.
     * When overriding this method, subsequent calls must return the same
     * object.
     * 
     * @return The provider for key managers for accessing protected resources
     *         (encryption).
     * @since  TrueZIP 7.3.
     */
    public KeyManagerProvider getKeyManagerProvider() {
        return KeyManagerLocator.SINGLETON;
    }

    /**
     * Returns the {@link RaesParameters} for the given file system model.
     * 
     * @param  model the file system model.
     * @return The {@link RaesParameters} for the given file system model.
     */
    final ZipCryptoParameters zipCryptoParameters(FsModel model) {
        return new KeyManagerZipCryptoParameters(
                getKeyManagerProvider(),
                mountPointUri(model));
    }

    /**
     * Returns a URI which represents the mount point of the given model as a
     * resource URI for looking up a {@link KeyProvider}.
     * Note that this URI needs to be matched exactly when setting a password
     * programmatically!
     * <p>
     * The implementation in the class {@link ZipDriver} returns the
     * expression {@code model.getMountPoint().toHierarchicalUri()}
     * in order to improve the readability of the URI in comparison to the
     * expression {@code model.getMountPoint().toUri()}.
     * 
     * @param  model the file system model.
     * @return A URI representing the file system model's mount point.
     * @see    <a href="http://java.net/jira/browse/TRUEZIP-72">#TRUEZIP-72</a>
     */
    public URI mountPointUri(FsModel model) {
        return model.getMountPoint().toHierarchicalUri();
    }

    /**
     * {@inheritDoc}
     *
     * @return The implementation in the class {@link ZipDriver} returns
     *         {@code true} because when reading a ZIP file sequentially,
     *         each ZIP entry should &quot;override&quot; any previously read
     *         ZIP entry with an equal name.
     *         This holds true even if the central directory is used to access
     *         the ZIP entries in random order.
     */
    @Override
    public boolean getRedundantContentSupport() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return The implementation in the class {@link ZipDriver} returns
     *         {@code true} because when reading a ZIP file sequentially,
     *         each ZIP entry should &quot;override&quot; any previously read
     *         ZIP entry with an equal name.
     *         This holds true even if the central directory is used to access
     *         the ZIP entries in random order.
     */
    @Override
    public boolean getRedundantMetaDataSupport() {
        return true;
    }

    @Override
    protected final IOPool<?> getPool() {
        return ioPool;
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
    protected boolean getPreambled() {
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
    protected boolean getPostambled() {
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
    protected int getMethod() {
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
    protected int getLevel() {
        return Deflater.BEST_COMPRESSION;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newInputShop}.
     */
    @Override
    public InputShop<ZipArchiveEntry> newInputShop(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newInputShop(model, rof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    protected InputShop<ZipArchiveEntry> newInputShop(
            FsModel model,
            ReadOnlyFile rof)
    throws IOException {
        return new ZipInputShop(this, rof);
    }

    /**
     * This implementation modifies {@code options} in the following way before
     * it forwards the call to {@code controller}:
     * <ol>
     * <li>{@link FsOutputOption#STORE} is set.
     * <li>If {@link FsOutputOption#GROW} is set, {@link FsOutputOption#APPEND}
     *     gets set too, and {@link FsOutputOption#CACHE} gets cleared.
     * </ol>
     * <p>
     * The resulting output socket is then wrapped in a private nested class
     * for an upcast in {@link #newOutputShop}.
     * Thus, when overriding this method, {@link #newOutputShop} should get
     * overridden, too.
     * Otherwise, a class cast exception will get thrown in
     * {@link #newOutputShop}.
     */
    @Override
    public OptionOutputSocket getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        BitField<FsOutputOption> options2 = options.set(STORE);
        if (options2.get(GROW))
            options2 = options2.set(APPEND).clear(CACHE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options2, template),
                options); // use original options!
    }

    /**
     * This implementation first checks if {@link FsOutputOption#GROW} is set
     * for the given {@code output} socket.
     * If this is the case and the given {@code source} is not {@code null},
     * then it's marked for appending to it.
     * Then, an output stream is acquired from the given {@code output} socket
     * and the parameters are forwarded to {@link #newOutputShop(FsModel, OptionOutputSocket, ZipInputShop)}
     * and the result gets wrapped in a new {@link FsMultiplexedOutputShop}
     * which uses the current {@link #getPool}.
     */
    @Override
    public final OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OutputSocket<?> output,
            final @CheckForNull InputShop<ZipArchiveEntry> source)
    throws IOException {
        return newOutputShop0(
                model,
                (OptionOutputSocket) output,
                (ZipInputShop) source);
    }

    private OutputShop<ZipArchiveEntry> newOutputShop0(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final BitField<FsOutputOption> options = output.getOptions();
        if (null != source)
            source.setAppendee(options.get(GROW));
        return newOutputShop(model, output, source);
    }

    protected OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return newOutputShop(model, out, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    protected OutputShop<ZipArchiveEntry> newOutputShop(
            FsModel model,
            OutputStream out,
            @CheckForNull ZipInputShop source)
    throws IOException {
        return new FsMultiplexedOutputShop<ZipArchiveEntry>(
                new ZipOutputShop(this, model, out, source),
                getPool());
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
        if (mknod.get(ENCRYPT))
            entry.setEncrypted(true);
        return entry;
    }

    @Override
    public ZipArchiveEntry newEntry(String name) {
        return new ZipArchiveEntry(name);
    }

    protected ZipArchiveEntry newEntry(String name, ZipArchiveEntry template) {
        return new ZipArchiveEntry(name, template);
    }
}
