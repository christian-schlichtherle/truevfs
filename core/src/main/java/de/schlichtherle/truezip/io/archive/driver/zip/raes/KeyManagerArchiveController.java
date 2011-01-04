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

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.FSDecoratorController;
import de.schlichtherle.truezip.io.filesystem.FSController;
import de.schlichtherle.truezip.io.filesystem.FSEntry;
import de.schlichtherle.truezip.io.filesystem.FSEntryName;
import de.schlichtherle.truezip.io.filesystem.FSException;
import de.schlichtherle.truezip.io.filesystem.FSModel;
import de.schlichtherle.truezip.key.KeyManager;
import java.io.CharConversionException;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.filesystem.FSEntryName.*;

/**
 * This archive controller resets the key provider in the key manager if the
 * target RAES encrypted ZIP archive file gets deleted.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class KeyManagerArchiveController
extends FSDecoratorController<
        FSModel,
        FSController<? extends FSModel>> {

    private final ArchiveDriver<?> driver;

    /**
     * Constructs a new key manager archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    KeyManagerArchiveController(final FSController<?> controller,
                                final ArchiveDriver<?> driver) {
        super(controller);
        this.driver = driver;
    }

    @Override
    public final FSEntry getEntry(final FSEntryName name)
    throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FSException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!name.isRoot())
                return null;
            Entry entry = getParent().getEntry(
                    getModel()
                        .getMountPoint()
                        .getPath()
                        .resolve(name)
                        .getEntryName());
            if (null == entry)
                return null;
            // The entry exists, but we can't access it for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            while (entry instanceof ArchiveFileSystemEntry<?>)
                entry = ((ArchiveFileSystemEntry<?>) entry).getEntry();
            try {
                return ArchiveFileSystemEntry.create(ROOT, SPECIAL,
                        driver.newEntry(ROOT.toString(), SPECIAL, entry));
            } catch (CharConversionException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
        }
    }

    @Override
    public void unlink(FSEntryName name) throws IOException {
        delegate.unlink(name);
        if (name.isRoot())
            KeyManager.resetKeyProvider(getModel().getMountPoint().getUri());
    }
}
