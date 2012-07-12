/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import static net.truevfs.kernel.spec.FsAccessOption.CACHE;
import static net.truevfs.kernel.spec.FsAccessOption.COMPRESS;
import net.truevfs.kernel.spec.*;
import static net.truevfs.kernel.spec.cio.Entry.ALL_POSIX_ACCESS;
import static net.truevfs.kernel.spec.cio.Entry.ALL_POSIX_ENTITIES;
import net.truevfs.kernel.spec.cio.Entry.Access;
import static net.truevfs.kernel.spec.cio.Entry.Access.WRITE;
import net.truevfs.kernel.spec.cio.Entry.PosixEntity;
import static net.truevfs.kernel.spec.cio.Entry.Size.DATA;
import net.truevfs.kernel.spec.cio.Entry.Type;
import net.truevfs.kernel.spec.cio.*;
import net.truevfs.kernel.spec.sl.IoPoolLocator;
import net.truevfs.kernel.spec.util.BitField;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

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

    /** Default record size */
    static final int DEFAULT_RCDSIZE = 512;

    /** Default block size */
    static final int DEFAULT_BLKSIZE = DEFAULT_RCDSIZE * 20;

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
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} calls the
     * equally named method on the {@link IoPoolLocator#SINGLETON}.
     */
    @Override
    public IoPool<? extends IoBuffer<?>> getIoPool() {
        return IoPoolLocator.SINGLETON.ioPool();
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
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<TarDriverEntry> input)
    throws IOException {
        return new MultiplexingOutputService<>(getIoPool(),
                new TarOutputService(model, sink, this));
    }

    /**
     * Clears {@link FsAccessOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsInputSocketSource source(
            BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name) {
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
            final FsController<?> controller,
            final FsEntryName name) {
        // Leave FsAccessOption.STORE untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
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
