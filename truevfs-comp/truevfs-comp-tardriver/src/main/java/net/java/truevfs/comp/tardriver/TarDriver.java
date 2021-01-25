/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.cio.Entry;
import static net.java.truecommons.cio.Entry.ALL_POSIX_ACCESS;
import static net.java.truecommons.cio.Entry.ALL_POSIX_ENTITIES;
import net.java.truecommons.cio.Entry.Access;
import static net.java.truecommons.cio.Entry.Access.WRITE;
import net.java.truecommons.cio.Entry.PosixEntity;
import static net.java.truecommons.cio.Entry.Size.DATA;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputService;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.cio.OutputService;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOption.CACHE;
import static net.java.truevfs.kernel.spec.FsAccessOption.COMPRESS;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsInputSocketSource;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsOutputSocketSink;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

/**
 * An archive driver for Tape Archive files (TAR).
 * By default, TAR files use the US-ASCII character set for the encoding of
 * entry names.
 * This configuration pretty much constraints the applicability of this file
 * format to North American countries.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class TarDriver extends FsArchiveDriver<TarDriverEntry> {

    /**
     * The character set for entry names and comments, which is the default
     * character set.
     */
    public static final Charset TAR_CHARSET = Charset.defaultCharset();

    /**
     * {@inheritDoc}
     *
     * @return {@link #TAR_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return TAR_CHARSET;
    }

    final String getEncoding() {
        return getCharset().name();
    }

    /**
     * Returns {@code true} if writing PAX headers for non US-ASCII entry names
     * should be supported or not.
     * As of TrueVFS 0.10.7, the implementation in {@link TarDriver} returns
     * {@code true}.
     * In older versions, the behaviour was as if this method returned
     * {@code false}.
     *
     * @since TrueVFS 0.10.7
     */
    public boolean getAddPaxHeaderForNonAsciiNames() {
        return true;
    }

    /**
     * Returns the method to use for encoding entry names with
     * {@link TarConstants#NAMELEN} or more characters.
     * As of TrueVFS 0.10.7, the implementation in {@link TarDriver} returns
     * {@link TarArchiveOutputStream#LONGFILE_POSIX}.
     * In older versions, the implementation returned
     * {@link TarArchiveOutputStream#LONGFILE_GNU}.
     */
    public int getLongFileMode() {
        return TarArchiveOutputStream.LONGFILE_POSIX;
    }

    /**
     * Returns the method to use for writing entries of more than
     * {@link TarConstants#MAXSIZE} (8 GiB) size.
     * As of TrueVFS 0.10.7, the implementation in {@link TarDriver} returns
     * {@link TarArchiveOutputStream#BIGNUMBER_POSIX}.
     * In older versions, the behaviour was as if this method returned
     * {@link TarArchiveOutputStream#BIGNUMBER_ERROR}.
     *
     * @since TrueVFS 0.10.7
     */
    public int getBigNumberMode() {
        return TarArchiveOutputStream.BIGNUMBER_POSIX;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} calls
     * {@code IoBufferPoolLocator.SINGLETON.get()}.
     */
    @Override
    public IoBufferPool getPool() {
        return IoBufferPoolLocator.SINGLETON.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The implementation in the class {@link TarDriver} returns
     *         {@code true} because when reading a TAR file sequentially,
     *         each TAR entry should &quot;override&quot; any previously read
     *         TAR entry with an equal name.
     */
    @Override
    public boolean getRedundantContentSupport() {
        return true;
    }

    @Override
    protected InputService<TarDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource source)
    throws IOException {
        return new TarInputService(model, source, this);
    }

    @Override
    protected OutputService<TarDriverEntry> newOutput(
            FsModel model,
            FsOutputSocketSink sink,
            @CheckForNull @WillNotClose InputService<TarDriverEntry> input)
    throws IOException {
        return new MultiplexingOutputService<>(getPool(), new TarOutputService(model, sink, this));
    }

    /**
     * Clears {@link FsAccessOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsInputSocketSource source(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // The target archive file will be only used to extract the TAR entries
        // to a temporary file, so we don't need to put it into the selective
        // entry cache.
        options = options.clear(CACHE);
        return new FsInputSocketSource(options, controller.input(options, name));
    }

    /**
     * Sets {@link FsAccessOption#COMPRESS} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // Leave FsAccessOption.STORE untouched - the driver shall be given
        // opportunity to get its own preferences to sort out such a conflict.
        options = options.set(COMPRESS);
        return new FsOutputSocketSink(options,
                controller.output(options, name, null));
    }

    @Override
    public TarDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            String name,
            final Type type,
            final @CheckForNull Entry template) {
        name = normalize(name, type);
        final TarDriverEntry entry;
        if (template instanceof TarArchiveEntry) {
            entry = newEntry(name, (TarArchiveEntry) template);
        } else {
            entry = newEntry(name);
            if (null != template) {
                entry.setModTime(template.getTime(WRITE));
                entry.setSize(template.getSize(DATA));
                for (final Access access : ALL_POSIX_ACCESS)
                    for (final PosixEntity entity : ALL_POSIX_ENTITIES)
                        entry.setPermitted(access, entity, template.isPermitted(access, entity));
            }
        }
        return entry;
    }

    public TarDriverEntry newEntry(String name) {
        return new TarDriverEntry(name);
    }

    public TarDriverEntry newEntry(String name, TarArchiveEntry template) {
        return new TarDriverEntry(name, template);
    }
}
