/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.entry.CommonEntryFactory;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.util.BitField;

/**
 * A read-only archive file system.
 * <p>
 * All modifying methods throw a {@link ReadOnlyArchiveFileSystemException}.
 *
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class ReadOnlyArchiveFileSystem<AE extends ArchiveEntry>
extends ReadWriteArchiveFileSystem<AE> {

    ReadOnlyArchiveFileSystem(
        final CommonEntryContainer<AE> container,
        final CommonEntryFactory<AE> factory,
        final CommonEntry rootTemplate) {
        super(container, factory, rootTemplate, null);
    }

    /** The implementation in this class returns {@code true}. */
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
    public ArchiveFileSystemOperation<AE> mknod( String path, Type type,
                                boolean createParents, CommonEntry template)
    throws ArchiveFileSystemException {
        throw new ReadOnlyArchiveFileSystemException();
    }

    @Override
    public void unlink(String path) throws ArchiveFileSystemException {
        throw new ReadOnlyArchiveFileSystemException();
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws ArchiveFileSystemException {
        throw new ReadOnlyArchiveFileSystemException();
    }
}
