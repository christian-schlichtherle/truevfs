/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Type.SPECIAL;
import static de.schlichtherle.truezip.fs.FsEntryName.ROOT;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.fs.archive.zip.KeyManagerController;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key required for accessing its target
 * RAES encrypted ZIP archive file (ZIP.RAES).
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipRaesController
extends KeyManagerController<ZipRaesDriver> {

    private static final String ROOT_PATH = ROOT.getPath();

    /**
     * Constructs a new ZIP.RAES archive controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP.RAES driver.
     */
    ZipRaesController(
            final FsController<?> controller,
            final ZipRaesDriver driver) {
        super(controller, driver);
    }

    @Override
    protected Class<?> getKeyType() {
        return AesCipherParameters.class;
    }

    @Override
    protected Class<? extends IOException> getKeyExceptionType() {
        return RaesKeyException.class;
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
            // We're not holding any locks, so it's possible that someone else
            // has concurrently modified the parent file system.
            if (null == entry)
                return null;
            // The entry is inaccessible for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            while (entry instanceof FsCovariantEntry<?>)
                entry = ((FsCovariantEntry<?>) entry).getEntry();
            final FsCovariantEntry<FsArchiveEntry>
                    special = new FsCovariantEntry<FsArchiveEntry>(ROOT_PATH);
            special.putEntry(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, entry));
            return special;
        }
    }
}
