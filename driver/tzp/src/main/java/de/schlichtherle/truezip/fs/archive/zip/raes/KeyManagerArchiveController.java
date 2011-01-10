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
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.fs.archive.ArchiveDriver;
import de.schlichtherle.truezip.fs.archive.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsException;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.key.KeyManager;
import java.io.CharConversionException;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;

/**
 * This archive controller resets the key provider in the key manager if the
 * target RAES encrypted ZIP archive file gets deleted.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class KeyManagerArchiveController
extends FsDecoratingController<
        FsModel,
        FsController<? extends FsModel>> {

    private final ArchiveDriver<?> driver;

    /**
     * Constructs a new key manager archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    KeyManagerArchiveController(final FsController<?> controller,
                                final ArchiveDriver<?> driver) {
        super(controller);
        this.driver = driver;
    }

    @Override
    public final FsEntry getEntry(final FsEntryName name)
    throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FsException ex) {
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
    public void unlink(FsEntryName name) throws IOException {
        delegate.unlink(name);
        if (name.isRoot())
            KeyManager.resetKeyProvider(getModel().getMountPoint().getUri());
    }
}
