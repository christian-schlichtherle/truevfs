/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link FsReadOnlyArchiveFileSystemException}.
 *
 * @param  <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsReadOnlyArchiveFileSystem<E extends FsArchiveEntry>
extends FsArchiveFileSystem<E> {

    FsReadOnlyArchiveFileSystem(final @WillNotClose EntryContainer<E> archive,
                                final FsArchiveDriver<E> driver,
                                final @CheckForNull Entry rootTemplate) {
        super(driver, archive, rootTemplate);
    }

    /**
     * Returns {@code true} to indicate that this archive file system is
     * read-only.
     * 
     * @return {@code true}
     */
    @Override
    boolean isReadOnly() {
        return true;
    }

    @Override
    FsArchiveFileSystemOperation<E> mknod(
            FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            Entry template)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    void unlink(FsEntryName path)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path, BitField<Access> types, long value)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path, Map<Access, Long> times)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }
}
