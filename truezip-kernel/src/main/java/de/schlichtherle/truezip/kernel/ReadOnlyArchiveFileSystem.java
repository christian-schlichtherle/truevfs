/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.*;
import de.truezip.kernel.cio.Container;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.READ;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.util.BitField;
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

    ReadOnlyArchiveFileSystem(final @WillNotClose Container<E> archive,
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
    void checkAccess(FsEntryName name, BitField<Access> types)
    throws IOException {
        if (!types.isEmpty() && !READ_ONLY.equals(types))
            throw new FsReadOnlyFileSystemException();
        super.checkAccess(name, types);
    }

    @Override
    ArchiveFileSystemOperation<E> mknod(
            BitField<FsAccessOption> options, FsEntryName name, Entry.Type type, Entry template)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    void unlink(FsEntryName path,
                BitField<FsAccessOption> options)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path,
                    BitField<Access> types,
                    long value,
                    BitField<FsAccessOption> options)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(FsEntryName path,
                    Map<Access, Long> times,
                    BitField<FsAccessOption> options)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }
}