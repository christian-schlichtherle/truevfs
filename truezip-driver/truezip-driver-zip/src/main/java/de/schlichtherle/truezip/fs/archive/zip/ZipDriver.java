/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import de.schlichtherle.truezip.zip.*;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for ZIP files.
 * By default, ZIP files use the IBM437 character set for the encoding of entry
 * names and comments (unless the General Purpose Bit 11 is present in
 * accordance with appendix D of the
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>).
 * They also apply the date/time conversion rules according to
 * {@link DateTimeConverter#ZIP}.
 * This configuration pretty much constraints the applicability of this driver
 * to North American and Western European countries.
 * However, this driver generally provides best interoperability with third
 * party tools like the Windows Explorer, WinZip, 7-Zip etc.
 * To some extent this applies even outside these countries.
 * Therefore, while you should use this driver to access plain old ZIP files,
 * you should <em>not</em> use it for custom application file formats - use the
 * {@link JarDriver} instead in this case.
 * <p>
 * This driver does <em>not</em> check the CRC value of any entries in existing
 * archives - use {@link CheckedZipDriver} instead.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class ZipDriver
extends FsCharsetArchiveDriver<ZipDriverEntry>
implements ZipOutputStreamParameters, ZipFileParameters<ZipDriverEntry> {

    private static final Logger logger = Logger.getLogger(
            ZipDriver.class.getName(),
            ZipDriver.class.getName());

    /**
     * The character set for entry names and comments in &quot;traditional&quot;
     * ZIP files, which is {@code "IBM437"}.
     */
    private static final Charset ZIP_CHARSET = Charset.forName("IBM437");

    private final IOPool<?> ioPool;

    /**
     * Constructs a new ZIP driver.
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
     * Constructs a new ZIP driver.
     *
     * @param provider the provider for the I/O buffer pool.
     * @param charset the character set for encoding entry names and comments.
     */
    protected ZipDriver(IOPoolProvider provider, Charset charset) {
        super(charset);
        if (null == (this.ioPool = provider.get()))
            throw new NullPointerException();
    }

    /**
     * Returns the key provider sync strategy.
     * The implementation in the class {@link ZipDriver} returns 
     * {@link KeyProviderSyncStrategy#RESET_CANCELLED_KEY}.
     *
     * @return The key provider sync strategy.
     */
    protected KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return KeyProviderSyncStrategy.RESET_CANCELLED_KEY;
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
    protected KeyManagerProvider getKeyManagerProvider() {
        return KeyManagerLocator.SINGLETON;
    }

    final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipInputShop input) {
        return zipCryptoParameters(input.getModel(), input.getRawCharset());
    }

    final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipOutputShop output) {
        return zipCryptoParameters(output.getModel(), output.getRawCharset());
    }

    /**
     * Returns the ZIP crypto parameters for the given file system model
     * and character set or {@code null} if not available.
     * To enable the use of this method when writing an archive entry with the
     * client APIs, you must use {@link FsOutputOption#ENCRYPT}.
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@code new KeyManagerZipCryptoParameters(getKeyManagerProvider(), mountPointUri(model), charset)}.
     * 
     * @param  model the file system model.
     * @param  charset charset the character set used for encoding entry names
     *         and the file comment in the ZIP file.
     * @return The ZIP crypto parameters for the given file system model
     *         and character set or {@code null} if not available.
     * @since  TrueZIP 7.3
     */
    protected @CheckForNull ZipCryptoParameters zipCryptoParameters(
            FsModel model,
            Charset charset) {
        return new KeyManagerZipCryptoParameters(this, model, charset);
    }

    /**
     * A template method which derives the URI which represents the mount point
     * of the given file system model as the base resource URI for looking up
     * {@link KeyProvider}s.
     * <p>
     * The implementation in the class {@link ZipDriver} returns the
     * expression {@code model.getMountPoint().toHierarchicalUri()}
     * in order to improve the readability of the URI in comparison to the
     * expression {@code model.getMountPoint().toUri()}.
     * 
     * @param  model the file system model.
     * @return The URI which represents the file system model's mount point.
     * @see    <a href="http://java.net/jira/browse/TRUEZIP-72">#TRUEZIP-72</a>
     */
    public URI mountPointUri(FsModel model) {
        return model.getMountPoint().toHierarchicalUri();
    }

    /**
     * A template method which derives the resource URI for looking up a
     * {@link KeyProvider} from the given file system model and entry name.
     * <p>
     * The implementation in the class {@code ZipDriver} ignores the given
     * entry name and just returns the expression {@code mountPointUri(model)}
     * in order to lookup the same key provider for all entries in a ZIP file.
     * <p>
     * An alternative implementation in a sub-class could return the expression
     * {@code mountPointUri(model).resolve("/" + name)} instead.
     * 
     * @param  model the file system model.
     * @param  name the entry name.
     * @return The URI for looking up a {@link KeyProvider}.
     */
    public URI resourceUri(FsModel model, String name) {
        //return mountPointUri(model).resolve("/" + name);
        return mountPointUri(model);
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
     * @since  TrueZIP 7.3
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
     * @since  TrueZIP 7.3
     */
    @Override
    public boolean getRedundantMetaDataSupport() {
        return true;
    }

    /**
     * Whether or not the content of the given entry shall get
     * checked/authenticated when reading it.
     * If this method returns {@code true} and the check fails,
     * then an {@link IOException} gets thrown.
     * 
     * @return {@code entry.isEncrypted()}.
     * @since  TrueZIP 7.3
     */
    protected boolean check(
            @WillNotClose ZipInputShop input,
            ZipDriverEntry entry) {
        return entry.isEncrypted();
    }

    final boolean process(
            @WillNotClose ZipInputShop input,
            ZipDriverEntry local,
            ZipDriverEntry peer) {
        return process(local, peer);
    }

    final boolean process(
            @WillNotClose ZipOutputShop output,
            ZipDriverEntry local,
            ZipDriverEntry peer) {
        return process(peer, local);
    }

    /**
     * Returns {@code true} if and only if the content of the given input
     * target entry needs processing when it gets copied to the given output
     * target entry.
     * This method gets called twice (once on each side of a copy operation)
     * and should return {@code false} unless both target entries can mutually
     * agree on transferring raw (unprocessed) content.
     * Note that it is an error to compare the properties of the target entries
     * because this method may get called before the local output target gets
     * mutated to compare equal with the peer input target!
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@code local.isEncrypted() || peer.isEncrypted()} in order to cover the
     * typical case that the cipher keys of both targets are not the same.
     * Note that there is no secure way to explicitly test for this.
     * 
     * @param  input the input target entry for copying the contents.
     * @param  output the output target entry for copying the contents.
     * @return Whether the content to get copied from the input target entry
     *         to the output target entry needs to get processed or can get
     *         sent in raw format.
     * @since  TrueZIP 7.3
     */
    protected boolean process(ZipDriverEntry input, ZipDriverEntry output) {
        return input.isEncrypted() || output.isEncrypted();
    }

    @Override
    protected final IOPool<?> getPool() {
        return ioPool;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code false}.
     * 
     * @return {@code false}
     */
    @Override
    public boolean getPreambled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code false}.
     * 
     * @return {@code false}
     */
    @Override
    public boolean getPostambled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code ZipOutputStream#OVERHEAD_SIZE}.
     * 
     * @since      TrueZIP 7.3
     * @return     {@code ZipOutputStream#OVERHEAD_SIZE}
     * @deprecated This method is reserved for future use - do <em>not</em> use
     *             or override this method!
     */
    @Deprecated
    @Override
    public int getOverheadSize() {
        return ZipOutputStream.OVERHEAD_SIZE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code ZipEntry#DEFLATED}.
     * 
     * @return {@code ZipEntry#DEFLATED}
     */
    @Override
    public int getMethod() {
        return DEFLATED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code Deflater#BEST_COMPRESSION}.
     * 
     * @return {@code Deflater#BEST_COMPRESSION}
     */
    @Override
    public int getLevel() {
        return Deflater.BEST_COMPRESSION;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver} returns the expression
     * {@code new ZipController(superNewController(model, parent), this)}.
     * This method should be overridden in order to call only
     * {@link #superNewController} if and only if you are overriding
     * {@link #zipCryptoParameters(de.schlichtherle.truezip.fs.FsModel, java.nio.charset.Charset)}, too,
     * and do not want to use the key manager to resolve passwords,
     * e.g. for WinZip AES encryption.
     */
    @Override
    public FsController<?>
    newController(FsModel model, FsController<?> parent) {
        return new ZipController(superNewController(model, parent), this);
    }

    /**
     * Equivalent to calling
     * {@link FsArchiveDriver#newController} on the {@link FsArchiveDriver}
     * class.
     * Call this method when overriding {@link #newController} and you need the
     * default file system controller chain instead of the implementation in
     * the class {@link ZipDriver}.
     */
    protected final FsController<?>
    superNewController(FsModel model, FsController<?> parent) {
        return super.newController(model, parent);
    }

    @Override
    public ZipDriverEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsOutputOption> mknod)
    throws CharConversionException {
        assertEncodable(name);
        name = toZipOrTarEntryName(name, type);
        final ZipDriverEntry entry;
        if (template instanceof ZipEntry) {
            entry = newEntry(name, (ZipEntry) template);
        } else {
            entry = newEntry(name);
            if (null != template) {
                entry.setTime(template.getTime(WRITE));
                entry.setSize(template.getSize(DATA));
            }
        }
        if (mknod.get(COMPRESS))
            entry.setMethod(DEFLATED);
        else if (mknod.get(STORE))
            entry.setMethod(STORED);
        if (DIRECTORY != type) {
            if (UNKNOWN == entry.getMethod()) {
                final int method = getMethod();
                entry.setMethod(method);
                if (STORED != method)
                    entry.setCompressedSize(UNKNOWN);
            }
            if (mknod.get(ENCRYPT))
                entry.setEncrypted(true);
        }
        return entry;
    }

    /**
     * Returns a new ZIP archive entry with the given {@code name}.
     *
     * @param  name the entry name.
     * @return {@code new ZipDriverEntry(name)}
     */
    @Override
    public ZipDriverEntry newEntry(String name) {
        return new ZipDriverEntry(name);
    }

    /**
     * Returns a new ZIP archive entry with the given {@code name} and all
     * other properties copied from the given template.
     *
     * @param  name the entry name.
     * @return {@code new ZipDriverEntry(name, template)}
     */
    public ZipDriverEntry newEntry(String name, ZipEntry template) {
        return new ZipDriverEntry(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newInputShop}.
     */
    @Override
    public InputShop<ZipDriverEntry> newInputShop(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newInputShop(model, rof);
        } catch (final IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @CreatesObligation
    protected InputShop<ZipDriverEntry> newInputShop(
            FsModel model,
            @WillCloseWhenClosed ReadOnlyFile rof)
    throws IOException {
        assert null != model;
        final ZipInputShop input = new ZipInputShop(this, model, rof);
        try {
            input.recoverLostEntries();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "junkInTheTrunk.warning", new Object[] {
                mountPointUri(model),
                input.getPostambleLength(),
            });
            logger.log(Level.FINE, "junkInTheTrunk.fine", ex);
        }
        return input;
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
            BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        if (options.get(GROW))
            options = options.set(APPEND).clear(CACHE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options, template),
                options);
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
    public final OutputShop<ZipDriverEntry> newOutputShop(
            final FsModel model,
            final OutputSocket<?> output,
            final InputShop<ZipDriverEntry> source)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        return newOutputShop0(
                model,
                (OptionOutputSocket) output,
                (ZipInputShop) source);
    }

    @CreatesObligation
    private OutputShop<ZipDriverEntry> newOutputShop0(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull @WillNotClose ZipInputShop source)
    throws IOException {
        final BitField<FsOutputOption> options = output.getOptions();
        if (null != source)
            source.setAppendee(options.get(GROW));
        return newOutputShop(model, output, source);
    }

    @CreatesObligation
    protected OutputShop<ZipDriverEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull @WillNotClose ZipInputShop source)
    throws IOException {
        assert null != model;
        final OutputStream out = output.newOutputStream();
        try {
            return newOutputShop(model, out, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected OutputShop<ZipDriverEntry> newOutputShop(
            FsModel model,
            @WillCloseWhenClosed OutputStream out,
            @CheckForNull @WillNotClose ZipInputShop source)
    throws IOException {
        return new FsMultiplexedOutputShop<ZipDriverEntry>(
                new ZipOutputShop(this, model, out, source),
                getPool());
    }
}
