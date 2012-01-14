/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * A decorating file system controller which unlinks the target archive file
 * from the parent file system if and only if the virtual root directory has
 * been successfully unlinked from its federated file system before.
 * 
 * @param   <M> The type of the file system model shared by the decorator chain
 *          of file system controllers.
 * @since   TrueZIP 7.4.4
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsUnlinkController
extends FsDecoratingLockModelController<FsController<? extends FsLockModel>> {

    /**
     * Constructs a new file system unlink controller.
     *
     * @param controller the decorated file system unlink controller.
     */
    FsUnlinkController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        delegate.unlink(name, options);
        final FsController<?> parent;
        if (name.isRoot() && null != (parent = getParent())) {
            // We have just removed the virtual root directory of a
            // federated file system, i.e. an archive file.
            // Now unlink the target archive file from the parent file system.
            parent.unlink(
                    getMountPoint().getPath().resolve(name).getEntryName(),
                    options);
        }
    }
}
