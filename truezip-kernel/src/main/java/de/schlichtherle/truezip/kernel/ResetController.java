/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsEntryName;
import static de.truezip.kernel.FsSyncOptions.RESET;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Resets the decorated file system controller once the virtual root directory
 * of the file system has been successfully unlinked.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class ResetController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    /**
     * Constructs a new file system unlink controller.
     *
     * @param controller the decorated file system controller.
     */
    ResetController(FsController<? extends LockModel> controller) {
        super(controller);
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsAccessOption> options)
    throws IOException {
        controller.unlink(name, options);
        if (name.isRoot()) {
            // The virtual root directory has been successfully removed.
            // Now the selective entry cache needs to get released without
            // flushing it.
            controller.sync(RESET);
        }
    }
}
