/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import static net.truevfs.kernel.FsAccessOption.CACHE;
import static net.truevfs.kernel.FsAccessOption.COMPRESS;
import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Entry;
import static net.truevfs.kernel.cio.Entry.ALL_POSIX_ACCESS;
import static net.truevfs.kernel.cio.Entry.ALL_POSIX_ENTITIES;
import net.truevfs.kernel.cio.Entry.Access;
import static net.truevfs.kernel.cio.Entry.Access.WRITE;
import net.truevfs.kernel.cio.Entry.PosixEntity;
import static net.truevfs.kernel.cio.Entry.Size.DATA;
import net.truevfs.kernel.cio.Entry.Type;
import net.truevfs.kernel.cio.InputService;
import net.truevfs.kernel.cio.MultiplexingOutputService;
import net.truevfs.kernel.cio.OutputService;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.util.BitField;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
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
            final Source source)
    throws IOException {
        return new TarInputService(model, source, this);
    }

    @Override
    protected OutputService<TarDriverEntry> newOutput(
            final FsModel model,
            final Sink sink,
            final @CheckForNull @WillNotClose InputService<TarDriverEntry> input)
    throws IOException {
        return new MultiplexingOutputService<>(getIOPool(),
                new TarOutputService(model, sink, this));
    }

    /**
     * Clears {@link FsAccessOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected Source source(
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name) {
        return controller.input(options.clear(CACHE), name);
    }

    /**
     * Sets {@link FsAccessOption#COMPRESS} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected Sink sink(
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name) {
        return controller.output(options.set(COMPRESS), name, null);
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
