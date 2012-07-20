/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.core;

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
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.driver.zip.core.io.ZipCryptoParameters;
import net.truevfs.driver.zip.core.io.ZipEntry;
import static net.truevfs.driver.zip.core.io.ZipEntry.*;
import net.truevfs.driver.zip.core.io.ZipFileParameters;
import net.truevfs.driver.zip.core.io.ZipOutputStreamParameters;
import static net.truevfs.kernel.spec.FsAccessOption.*;
import net.truevfs.kernel.spec.*;
import static net.truevfs.kernel.spec.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.spec.cio.Entry.Size.DATA;
import net.truevfs.kernel.spec.cio.Entry.Type;
import static net.truevfs.kernel.spec.cio.Entry.Type.DIRECTORY;
import net.truevfs.kernel.spec.cio.*;
import net.truevfs.kernel.spec.sl.IoBufferPoolLocator;
import net.truevfs.kernel.spec.util.BitField;
import net.truevfs.kernel.spec.util.HashMaps;
import net.truevfs.keymgr.spec.KeyManagerContainer;
import net.truevfs.keymgr.spec.KeyProvider;
import net.truevfs.keymgr.spec.sl.KeyManagerMapLocator;

/**
 * An abstract archive driver for the ZIP file format.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class AbstractZipDriver
extends FsArchiveDriver<AbstractZipDriverEntry>
implements ZipOutputStreamParameters, ZipFileParameters<AbstractZipDriverEntry> {

    private static final Logger logger = Logger.getLogger(
            AbstractZipDriver.class.getName(),
            AbstractZipDriver.class.getName());

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} calls the
     * equally named method on the {@link IoBufferPoolLocator#SINGLETON}.
     */
    @Override
    public IoBufferPool<? extends IoBuffer<?>> getPool() {
        return IoBufferPoolLocator.SINGLETON.get();
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
    public KeyManagerContainer getKeyManagerProvider() {
        return KeyManagerMapLocator.SINGLETON;
    }

    public final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipInputService input) {
        return zipCryptoParameters(input.getModel(), input.getRawCharset());
    }

    public final @CheckForNull ZipCryptoParameters zipCryptoParameters(ZipOutputService output) {
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
    public boolean check(
            AbstractZipDriverEntry entry,
            @WillNotClose ZipInputService input) {
        return entry.isEncrypted();
    }

    public final boolean rdc(
            @WillNotClose ZipInputService input,
            AbstractZipDriverEntry local,
            AbstractZipDriverEntry peer) {
        return rdc(local, peer);
    }

    public final boolean rdc(
            @WillNotClose ZipOutputService output,
            AbstractZipDriverEntry local,
            AbstractZipDriverEntry peer) {
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
    protected boolean rdc(AbstractZipDriverEntry input, AbstractZipDriverEntry output) {
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
     * This should pool overridden in order to return just {@code controller} if
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
            final FsInputSocketSource source)
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
            FsInputSocketSource source)
    throws IOException {
        assert null != model;
        return new ZipInputService(model, source, this);
    }

    @Override
    @CreatesObligation
    protected OutputService<AbstractZipDriverEntry> newOutput(
            FsModel model,
            FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<AbstractZipDriverEntry> input)
    throws IOException {
        final ZipInputService zis = (ZipInputService) input;
        return new MultiplexingOutputService<>(getPool(),
                new ZipOutputService(model, sink, zis, this));
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
     * Otherwise, a class cast exception will pool thrown in
     * {@link #newOutput}.
     */
    @Override
    protected FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to get its own preferences to sort out such a conflict.
        options = options.set(STORE);
        if (options.get(GROW))
            options = options.set(APPEND).clear(CACHE);
        return new FsOutputSocketSink(options,
                controller.output(options, name, null));
    }

    @Override
    public AbstractZipDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            String name,
            final Type type,
            final @CheckForNull Entry template) {
        name = normalize(name, type);
        final AbstractZipDriverEntry entry;
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
     * Returns a new ZIP driver entry with the given {@code name}.
     *
     * @param  name the entry name.
     * @return {@code new AbstractZipDriverEntry(name)}
     */
    @Override
    public abstract AbstractZipDriverEntry newEntry(String name);

    /**
     * Returns a new ZIP driver entry with the given {@code name} and all
     * other properties copied from the given template.
     *
     * @param  name the entry name.
     * @param  template the entry template.
     * @return {@code new AbstractZipDriverEntry(name, template)}
     */
    public abstract AbstractZipDriverEntry newEntry(String name, ZipEntry template);
}
