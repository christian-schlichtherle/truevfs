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
package de.schlichtherle.truezip.io.archive.driver.zip.raes;

import java.io.CharConversionException;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import java.util.Set;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.FilterEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipEntry;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController;
import de.schlichtherle.truezip.io.archive.controller.FilterArchiveController;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.Paths.isRoot;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;

/**
 * This archive controller resets the key provider in the key manager if the
 * target RAES encrypted ZIP archive file gets deleted.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class KeyManagerArchiveController
extends FilterArchiveController {

    private final ArchiveDriver<ZipEntry> driver;

    /**
     * Constructs a new key manager archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    KeyManagerArchiveController(
            final ArchiveController controller,
            final ArchiveDriver<ZipEntry> driver) {
        super(controller);
        this.driver = driver;
    }

    @Override
    public final FileSystemEntry getEntry(final FileSystemEntryName name)
    throws IOException {
        try {
            return controller.getEntry(name);
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!isRoot(name.getPath()))
                return null;
            final FileSystemEntry entry = getParent()
                    .getEntry(getModel().resolveParent(name));
            if (null == entry)
                return null;
            // The entry exists, but we can't access it for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            try {
                return new SpecialFileEntry<ZipEntry>(
                        driver.newEntry(ROOT, SPECIAL,
                            entry instanceof ArchiveFileSystemEntry<?>
                                ? ((ArchiveFileSystemEntry<?>) entry).getArchiveEntry()
                                : entry));
            } catch (CharConversionException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
        }
    }

    private static final class SpecialFileEntry<E extends ArchiveEntry>
    extends FilterEntry<E>
    implements ArchiveFileSystemEntry<E> {
        SpecialFileEntry(E entry) {
            super(entry);
        }

        @Override
        public Entry.Type getType() {
            return SPECIAL; // drivers could ignore this type, so we must ignore them!
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }

        @Override
        public E getArchiveEntry() {
            return entry;
        }
    }

    @Override
    public void unlink(FileSystemEntryName path) throws IOException {
        controller.unlink(path);
        if (isRoot(path.getPath()))
            KeyManager.resetKeyProvider(getModel().getMountPoint().getUri());
    }
}
