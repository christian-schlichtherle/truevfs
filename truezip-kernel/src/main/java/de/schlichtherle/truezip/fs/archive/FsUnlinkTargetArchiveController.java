/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Unlinks the target archive file from the parent file system once the virtual
 * root directory of the federated file system has been successfully unlinked.
 * 
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@Immutable
final class FsUnlinkTargetArchiveController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    /**
     * Constructs a new file system unlink target archive controller.
     *
     * @param controller the decorated file system controller.
     */
    FsUnlinkTargetArchiveController(FsController<? extends M> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        delegate.unlink(name, options);
        if (name.isRoot()) {
            // We have successfully removed the virtual root directory of a
            // federated file system, i.e. an archive file.
            // Now unlink the target archive file from the parent file system.
            // TODO: If this fails then this operation will NOT be atomic!
            getParent().unlink(
                    getMountPoint().getPath().resolve(name).getEntryName(),
                    options);
        }
    }
}
