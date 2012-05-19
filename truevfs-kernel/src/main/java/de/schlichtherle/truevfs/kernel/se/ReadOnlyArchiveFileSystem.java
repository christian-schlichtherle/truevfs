/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Container;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.Entry.Access;
import static net.truevfs.kernel.cio.Entry.Access.READ;
import net.truevfs.kernel.util.BitField;
import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link FsReadOnlyFileSystemException}.
 *
 * @param  <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class ReadOnlyArchiveFileSystem<E extends FsArchiveEntry>
extends ArchiveFileSystem<E> {

    private static final BitField<Access> READ_ONLY = BitField.of(READ);

    ReadOnlyArchiveFileSystem(
            final FsArchiveDriver<E> driver,
            final @WillNotClose Container<E> archive,
            final @CheckForNull Entry rootTemplate) {
        super(driver, archive, rootTemplate);
    }

    @Override
    void checkAccess(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types)
    throws IOException {
        if (!types.isEmpty() && !READ_ONLY.equals(types))
            throw new FsReadOnlyFileSystemException();
        super.checkAccess(options, name, types);
    }

    @Override
    void setReadOnly(FsEntryName name) { }

    @Override
    boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Map<Access, Long> times)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types,
            long value)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    Mknod mknod(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Entry.Type type,
            Entry template)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    void unlink(BitField<FsAccessOption> options, FsEntryName name)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }
}
