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

import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.fs.archive.FsArchiveFileSystemEntry;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
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
final class ZipRaesArchiveController
extends FsDecoratingController<FsModel, FsController<?>> {

    private final ZipRaesDriver driver;

    /**
     * Constructs a new key manager archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    ZipRaesArchiveController(   final FsController<?> controller,
                                final ZipRaesDriver driver) {
        super(controller);
        this.driver = driver;
    }

    @Override
    public final FsEntry getEntry(final FsEntryName name)
    throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (final FsFalsePositiveException ex) {
            if (!(ex.getCause() instanceof RaesKeyException))
                throw ex;
            if (!name.isRoot())
                return null;
            Entry entry = getParent().getEntry(
                    getModel()
                        .getMountPoint()
                        .getPath()
                        .resolve(name)
                        .getEntryName());
            assert null != entry;
            // The entry is inaccessible for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            while (entry instanceof FsArchiveFileSystemEntry<?>)
                entry = ((FsArchiveFileSystemEntry<?>) entry).getEntry();
            try {
                return FsArchiveFileSystemEntry.create(ROOT, SPECIAL,
                        driver.newEntry(ROOT.getPath(), SPECIAL, entry));
            } catch (CharConversionException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
        }
    }

    @Override
    public void unlink(final FsEntryName name) throws IOException {
        try {
            delegate.unlink(name);
        } catch (final FsFalsePositiveException ex) {
            // If the false positive exception is caused by a RAES key
            // exception, then throw this instead in order to avoid delegating
            // this method to the parent file system.
            // This prevents the application from inadvertently deleting a
            // RAES encrypted ZIP file just because its key wasn't available
            // because e.g. the user has cancelled key prompting.
            final Throwable cause = ex.getCause();
            throw cause instanceof RaesKeyException
                    ? (RaesKeyException) cause
                    : ex;
        }
        if (name.isRoot())
            driver  .getKeyManagerService()
                    .get(Object.class)
                    .removeKeyProvider(getModel().getMountPoint().getUri());
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        delegate.sync(options, handler);
        driver  .getKeyProviderSyncStrategy()
                .sync(
                    driver  .getKeyManagerService()
                            .get(Object.class)
                            .getKeyProvider(
                                getModel().getMountPoint().getUri()));
    }
}
