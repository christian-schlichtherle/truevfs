/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link FsReadOnlyArchiveFileSystemException}.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
final class FsReadOnlyArchiveFileSystem<E extends FsArchiveEntry>
extends FsArchiveFileSystem<E> {

    FsReadOnlyArchiveFileSystem(final EntryContainer<E> archive,
                                final FsArchiveDriver<E> driver,
                                final Entry rootTemplate) {
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
