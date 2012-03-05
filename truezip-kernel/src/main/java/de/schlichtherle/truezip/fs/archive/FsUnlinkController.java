/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.*;
import static de.schlichtherle.truezip.fs.FsSyncOptions.CANCEL;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Unlinks the target archive file from the parent file system if and only if
 * the virtual root directory has been successfully unlinked from its federated
 * file system before.
 * 
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsUnlinkController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    /**
     * Constructs a new file system unlink controller.
     *
     * @param controller the decorated file system controller.
     */
    FsUnlinkController(FsController<? extends FsLockModel> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();

        if (name.isRoot()) {
            try {
                delegate.unlink(name, options);
            } finally {
                // Always clear the cache!
                // The only effect of calling sync for a false positive
                // archive file is that it will reset the mount state so
                // that the file system can be successfully mounted again
                // if the target archive file gets subsequently modified to
                // be a true archive file.
                // FIXME: If this fails then this operation will NOT be atomic!
                sync(CANCEL);
            }
            // We have just removed the virtual root directory of a
            // federated file system, i.e. an archive file.
            // Now unlink the target archive file from the parent file system.
            // FIXME: If this fails then this operation will NOT be atomic!
            getParent().unlink(
                    getMountPoint().getPath().resolve(name).getEntryName(),
                    options);
        } else {
            delegate.unlink(name, options);
        }
    }
}
