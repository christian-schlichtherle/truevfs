/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

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
@DefaultAnnotation(NonNull.class)
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
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isTouched() {
        assert !super.isTouched();
        return false;
    }

    @Override
    public FsArchiveFileSystemOperation<E> mknod(
            FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            Entry template)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    public void unlink(FsEntryName path)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    public boolean setTime(FsEntryName path, BitField<Access> types, long value)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }

    @Override
    public boolean setTime(FsEntryName path, Map<Access, Long> times)
    throws FsArchiveFileSystemException {
        throw new FsReadOnlyArchiveFileSystemException();
    }
}
