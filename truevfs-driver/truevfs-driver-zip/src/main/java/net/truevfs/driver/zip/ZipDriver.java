/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import static net.truevfs.driver.zip.io.ZipEntry.*;
import net.truevfs.driver.zip.io.*;
import static net.truevfs.kernel.FsAccessOption.*;
import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Entry;
import static net.truevfs.kernel.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.cio.Entry.Size.DATA;
import net.truevfs.kernel.cio.Entry.Type;
import static net.truevfs.kernel.cio.Entry.Type.DIRECTORY;
import net.truevfs.kernel.cio.InputService;
import net.truevfs.kernel.cio.MultiplexingOutputService;
import net.truevfs.kernel.cio.OutputService;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.util.BitField;
import net.truevfs.kernel.util.HashMaps;
import net.truevfs.key.KeyManagerProvider;
import net.truevfs.key.KeyProvider;
import net.truevfs.key.sl.KeyManagerLocator;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import javax.annotation.CheckForNull;
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
extends FsArchiveDriver<ZipDriverEntry>
implements ZipOutputStreamParameters, ZipFileParameters<ZipDriverEntry> {

    private static final Logger logger = Logger.getLogger(
            ZipDriver.class.getName(),
            ZipDriver.class.getName());

    /**
     * The character set for entry names and comments in &quot;traditional&quot;
     * ZIP files, which is {@code "IBM437"}.
     */
    private static final Charset ZIP_CHARSET = Charset.forName("IBM437");

    /**
     * {@inheritDoc}
     * 
     * @return {@link #ZIP_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return ZIP_CHARSET;
    }

    /**
     * Returns the provider for key managers for accessing protected resources
     * (encryption).
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     * 
     * @return {@link KeyManagerLocator#SINGLETON}, as by the implementation
     *         in the class {@link ZipDriver}.
     */
    public KeyManagerProvider getKeyManagerProvider() {
        return KeyManagerLocator.SINGLETON;
    }

    final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipInputService input) {
        return zipCryptoParameters(input.getModel(), input.getRawCharset());
    }

    final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipOutputService output) {
        return zipCryptoParameters(output.getModel(), output.getRawCharset());
    }

    /**
     * Returns the ZIP crypto parameters for the given file system model
     * and character set or {@code null} if not available.
     * To enable the use of this method when writing an archive entry with the
     * client APIs, you must use {@link FsAccessOption#ENCRYPT}.
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@code new KeyManagerZipCryptoParameters(getKeyManagerProvider(), mountPointUri(model), charset)}.
     * 
     * @param  model the file system model.
     * @param  charset charset the character set used for encoding entry names
     *         and the file comment in the ZIP file.
     * @return The ZIP crypto parameters for the given file system model
     *         and character set or {@code null} if not available.
     */
    protected @CheckForNull ZipCryptoParameters zipCryptoParameters(
            FsModel model,
            Charset charset) {
        return new KeyManagerZipCryptoParameters(this, model, charset);
    }

    /**
     * A template method for resolving the resource URI which is required to
     * look up the {@link KeyProvider} for the mount point of the file system
     * with the given model.
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
     * A template method for resolving the resource URI which is required to
     * look up the {@link KeyProvider} for the entry with the given name in the
     * file system with the given model.
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
    public URI fileSystemUri(FsModel model, String name) {
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

    /**
     * Whether or not the content of the given entry shall get
     * checked/authenticated when reading it.
     * If this method returns {@code true} and the check fails, then an
     * {@link IOException} gets thrown.
     * 
     * @param entry the entry to test.
     * @param input the origin of the entry.
     * @return {@code entry.isEncrypted()}.
     */
    protected boolean check(
            ZipDriverEntry entry,
            @WillNotClose ZipInputService input) {
        return entry.isEncrypted();
    }

    final boolean rdc(
            @WillNotClose ZipInputService input,
            ZipDriverEntry local,
            ZipDriverEntry peer) {
        return rdc(local, peer);
    }

    final boolean rdc(
            @WillNotClose ZipOutputService output,
            ZipDriverEntry local,
            ZipDriverEntry peer) {
        return rdc(peer, local);
    }

    /**
     * Returns {@code true} if and only if the content of the given input
     * target entry is eligible for Raw Data Copying (RDC).
     * This method gets called twice (once on each side of a copy operation)
     * and should return {@code false} unless both target entries can mutually
     * agree on transferring raw (unprocessed) content.
     * Note that it is an error to compare the properties of the target entries
     * because this method may get called <em>before</em> the output target
     * entry gets mutated to compare equal with the input target entry!
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@code !local.isEncrypted() && !remote.isEncrypted()} in order to cover
     * the typical case that the cipher keys of both targets are not the same.
     * Note that there is no secure way to explicitly test for this.
     * 
     * @param  input the input target entry for copying the contents.
     * @param  output the output target entry for copying the contents.
     * @return Whether the content to get copied from the input target entry
     *         to the output target entry is eligible for Raw Data Copying
     *         (RDC).
     */
    protected boolean rdc(ZipDriverEntry input, ZipDriverEntry output) {
        return !input.isEncrypted() && !output.isEncrypted();
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
     * returns {@code Maps#OVERHEAD_SIZE}.
     * 
     * @return {@code Maps#OVERHEAD_SIZE}
     */
    @Override
    public int getOverheadSize() {
        return HashMaps.OVERHEAD_SIZE;
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
     * The implementation in the class {@link ZipDriver} decorates the
     * given controller with a package private controller which keeps track of
     * the AES PBE parameters.
     * This should getIOPool overridden in order to return just {@code controller} if
     * and only if you are overriding
     * {@link #zipCryptoParameters(FsModel, Charset)}, too, and do not want to
     * use the locatable key manager to resolve passwords, e.g. for WinZip AES
     * encryption.
     */
    @Override
    public <M extends FsModel> FsController<M> decorate(
            FsController<M> controller) {
        return new ZipKeyController<>(controller, this);
    }

    @Override
    protected final ZipInputService newInput(
            final FsModel model,
            final Source source)
    throws IOException {
        final ZipInputService zis = newZipInput(Objects.requireNonNull(model), source);
        try {
            zis.recoverLostEntries();
        } catch (final IOException ex) {
            logger.log(Level.WARNING, "junkInTheTrunk.warning", new Object[] {
                mountPointUri(model),
                zis.getPostambleLength(),
            });
            logger.log(Level.FINE, "junkInTheTrunk.fine", ex);
        }
        return zis;
    }

    @CreatesObligation
    protected ZipInputService newZipInput(
            FsModel model,
            Source source)
    throws IOException {
        assert null != model;
        return new ZipInputService(model, source, this);
    }

    @Override
    public OutputService<ZipDriverEntry> newOutput(
            final FsModel model,
            final BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name,
            final @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        final OptionOutputSocket oos = sink(options, controller, name);
        final ZipInputService zis = (ZipInputService) input;
        if (null != zis)
            zis.setAppendee(oos.getOptions().get(GROW));
        return newOutput(model, oos, zis);
    }

    @Override
    @CreatesObligation
    protected OutputService<ZipDriverEntry> newOutput(
            FsModel model,
            Sink sink,
            final @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        final OptionOutputSocket oos = (OptionOutputSocket) sink;
        final ZipInputService zis = (ZipInputService) input;
        return new MultiplexingOutputService<>(getIOPool(),
                new ZipOutputService(model, oos, zis, this));
    }

    /**
     * This implementation modifies {@code options} in the following way before
     * it forwards the call to {@code controller}:
     * <ol>
     * <li>{@link FsAccessOption#STORE} is set.
     * <li>If {@link FsAccessOption#GROW} is set, then
     *     {@link FsAccessOption#APPEND} gets set, too, and
     *     {@link FsAccessOption#CACHE} gets cleared.
     * </ol>
     * <p>
     * The resulting output socket is then wrapped in a private nested class
     * for an upcast in {@link #newOutput}.
     * Thus, when overriding this method, {@link #newOutput} should get
     * overridden, too.
     * Otherwise, a class cast exception will getIOPool thrown in
     * {@link #newOutput}.
     */
    @Override
    protected OptionOutputSocket sink(
            BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        if (options.get(GROW))
            options = options.set(APPEND).clear(CACHE);
        return new OptionOutputSocket(
                controller.output(options, name, null),
                options);
    }

    @Override
    public ZipDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            String name,
            final Type type,
            final @CheckForNull Entry template) {
        name = normalize(name, type);
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
        if (options.get(COMPRESS))
            entry.setMethod(DEFLATED);
        else if (options.get(STORE))
            entry.setMethod(STORED);
        if (DIRECTORY != type) {
            if (UNKNOWN == entry.getMethod()) {
                final int method = getMethod();
                entry.setMethod(method);
                if (STORED != method)
                    entry.setCompressedSize(UNKNOWN);
            }
            if (options.get(ENCRYPT))
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
     * @param  template the entry template.
     * @return {@code new ZipDriverEntry(name, template)}
     */
    public ZipDriverEntry newEntry(String name, ZipEntry template) {
        return new ZipDriverEntry(name, template);
    }
}
