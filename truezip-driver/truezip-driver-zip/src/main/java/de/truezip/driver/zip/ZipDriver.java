/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import static de.truezip.driver.zip.io.ZipEntry.*;
import de.truezip.driver.zip.io.*;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.fs.FsCharsetArchiveDriver;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsAccessOption;
import static de.truezip.kernel.fs.option.FsAccessOption.*;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Maps;
import de.truezip.key.KeyManagerProvider;
import de.truezip.key.KeyProvider;
import de.truezip.key.sl.KeyManagerLocator;
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
     * Returns the provider for key managers for accessing protected resources
     * (encryption).
     * When overriding this method, repeated calls must return the same object.
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
     * If this method returns {@code true} and the check fails,
     * then an {@link IOException} gets thrown.
     * 
     * @return {@code entry.isEncrypted()}.
     */
    protected boolean check(
            @WillNotClose ZipInputService input,
            ZipDriverEntry entry) {
        return entry.isEncrypted();
    }

    final boolean process(
            @WillNotClose ZipInputService input,
            ZipDriverEntry local,
            ZipDriverEntry remote) {
        return process(local, remote);
    }

    final boolean process(
            @WillNotClose ZipOutputService output,
            ZipDriverEntry local,
            ZipDriverEntry remote) {
        return process(remote, local);
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
     * mutated to compare equal with the remote input target!
     * <p>
     * The implementation in the class {@link ZipDriver} returns
     * {@code local.isEncrypted() || remote.isEncrypted()} in order to cover the
     * typical case that the cipher keys of both targets are not the same.
     * Note that there is no secure way to explicitly test for this.
     * 
     * @param  input the input target entry for copying the contents.
     * @param  output the output target entry for copying the contents.
     * @return Whether the content to get copied from the input target entry
     *         to the output target entry needs to get processed or can get
     *         sent in raw format.
     */
    protected boolean process(ZipDriverEntry input, ZipDriverEntry output) {
        return input.isEncrypted() || output.isEncrypted();
    }

    @Override
    public final IOPool<?> getIOPool() {
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
     * returns {@code Maps#OVERHEAD_SIZE}.
     * 
     * @return {@code Maps#OVERHEAD_SIZE}
     */
    @Override
    public int getOverheadSize() {
        return Maps.OVERHEAD_SIZE;
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
     * This should get overridden in order to return just {@code controller} if
     * and only if you are overriding
     * {@link #zipCryptoParameters(FsModel, Charset)}, too, and do not want to
     * use the locatable key manager to resolve passwords, e.g. for WinZip AES
     * encryption.
     */
    @Override
    public <M extends FsModel> FsController<M> decorate(
            FsController<M> controller) {
        return new ZipKeyController<M>(controller, this);
    }

    @Override
    public ZipDriverEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsAccessOption> mknod)
    throws CharConversionException {
        checkEncodable(name);
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
     * {@link #newInputService}.
     */
    @Override
    public InputService<ZipDriverEntry> newInputService(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            return newInputService(model, rof);
        } catch (final IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @CreatesObligation
    protected InputService<ZipDriverEntry> newInputService(
            FsModel model,
            @WillCloseWhenClosed ReadOnlyFile rof)
    throws IOException {
        assert null != model;
        final ZipInputService input = new ZipInputService(this, model, rof);
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
     * <li>{@link FsAccessOption#STORE} is set.
     * <li>If {@link FsAccessOption#GROW} is set, {@link FsAccessOption#APPEND}
     *     gets set too, and {@link FsAccessOption#CACHE} gets cleared.
     * </ol>
     * <p>
     * The resulting output socket is then wrapped in a private nested class
     * for an upcast in {@link #newOutputService}.
     * Thus, when overriding this method, {@link #newOutputService} should get
     * overridden, too.
     * Otherwise, a class cast exception will get thrown in
     * {@link #newOutputService}.
     */
    @Override
    public OptionOutputSocket getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        if (options.get(GROW))
            options = options.set(APPEND).clear(CACHE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options, template),
                options);
    }

    /**
     * This implementation first checks if {@link FsAccessOption#GROW} is set
     * for the given {@code output} socket.
     * If this is the case and the given {@code source} is not {@code null},
     * then it's marked for appending to it.
     * Then, an output stream is acquired from the given {@code output} socket
     * and the parameters are forwarded to {@link #newOutputService(FsModel, OptionOutputSocket, ZipInputService)}
     * and the result gets wrapped in a new {@link MultiplexedOutputService}
     * which uses the current {@link #getIOPool}.
     */
    @Override
    public final OutputService<ZipDriverEntry> newOutputService(
            final FsModel model,
            final OutputSocket<?> output,
            final InputService<ZipDriverEntry> source)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        return newOutputService0(
                model,
                (OptionOutputSocket) output,
                (ZipInputService) source);
    }

    @CreatesObligation
    private OutputService<ZipDriverEntry> newOutputService0(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull @WillNotClose ZipInputService source)
    throws IOException {
        final BitField<FsAccessOption> options = output.getOptions();
        if (null != source)
            source.setAppendee(options.get(GROW));
        return newOutputService(model, output, source);
    }

    @CreatesObligation
    protected OutputService<ZipDriverEntry> newOutputService(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull @WillNotClose ZipInputService source)
    throws IOException {
        assert null != model;
        final OutputStream out = output.newOutputStream();
        try {
            return newOutputService(model, out, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected OutputService<ZipDriverEntry> newOutputService(
            FsModel model,
            @WillCloseWhenClosed OutputStream out,
            @CheckForNull @WillNotClose ZipInputService source)
    throws IOException {
        return new MultiplexedOutputService<ZipDriverEntry>(
                new ZipOutputService(this, model, out, source),
                getIOPool());
    }
}