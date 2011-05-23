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
import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.archive.FsArchiveDriver.*;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.fs.archive.FsCovariantEntry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key required for accessing its target
 * RAES encrypted ZIP archive file (ZIP.RAES).
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class ZipRaesController
extends FsDecoratingController<FsModel, FsController<?>> {

    private static final String ROOT_PATH = ROOT.getPath();

    private final ZipRaesDriver driver;

    /**
     * Constructs a new ZIP.RAES archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     * @param driver the ZIP.RAES driver.
     */
    ZipRaesController(  final FsController<?> controller,
                        final ZipRaesDriver driver) {
        super(controller);
        this.driver = driver;
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
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
            while (entry instanceof FsCovariantEntry<?>)
                entry = ((FsCovariantEntry<?>) entry).getEntry();
            final FsCovariantEntry<FsArchiveEntry>
                    special = new FsCovariantEntry<FsArchiveEntry>(ROOT_PATH);
            special.putEntry(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, entry, NO_OUTPUT_OPTION));
            return special;
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
            driver  .getKeyManager()
                    .removeKeyProvider(
                        driver.toMountPointResource(getModel()));
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        delegate.sync(options, handler);
        driver  .getKeyProviderSyncStrategy()
                .sync(
                    driver  .getKeyManager()
                            .getKeyProvider(
                                driver.toMountPointResource(getModel())));
    }
}
