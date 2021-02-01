/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.kernel.spec.FsController;
import global.namespace.truevfs.kernel.spec.FsSyncException;

import java.io.IOException;

/**
 * Calls back the given pace manager after each file system operation in order to register itself as the most recently
 * accessed file system and unmount the least recently accessed file systems which exceed the maximum number of mounted
 * file systems.
 *
 * @author Christian Schlichtherle
 */
class PaceController extends AspectController {

    private final PaceManager manager;

    PaceController(PaceManager manager, FsController controller) {
        super(controller);
        this.manager = manager;
    }

    @Override
    <T> T apply(final Op<T> op) throws IOException {
        IOException t1 = null;
        try {
            return op.call();
        } catch (final IOException t2) {
            t1 = t2;
            throw t2;
        } finally {
            try {
                manager.recordAccess(getMountPoint());
            } catch (final FsSyncException t2) {
                if (null == t1) {
                    throw t2;
                }
                t1.addSuppressed(t2);
            }
        }
    }
}
