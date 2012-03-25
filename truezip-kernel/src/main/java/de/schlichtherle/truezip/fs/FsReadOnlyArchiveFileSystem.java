/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.addr.FsEntryName;
import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.cio.Entry.Access;
import de.schlichtherle.truezip.cio.Entry.Type;
import de.schlichtherle.truezip.cio.Container;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
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
final class FsReadOnlyArchiveFileSystem<E extends FsArchiveEntry>
extends FsArchiveFileSystem<E> {

    FsReadOnlyArchiveFileSystem(final @WillNotClose Container<E> archive,
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
    throws FsFileSystemException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    void unlink(FsEntryName path)
    throws FsFileSystemException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path, BitField<Access> types, long value)
    throws FsFileSystemException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path, Map<Access, Long> times)
    throws FsFileSystemException {
        throw new FsReadOnlyFileSystemException();
    }
}
