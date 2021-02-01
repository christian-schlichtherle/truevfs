/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.shed.BitField;

import java.io.IOException;

/**
 * Indicates that a file system is a false positive file system and that this exception may get cached until the
 * federated (archive) file system gets {@linkplain global.namespace.truevfs.kernel.spec.FsController#sync(BitField) synced}
 * again.
 * <p>
 * This exception type is reserved for non-local control flow in file system controller chains in order to reroute file
 * system operations to the parent file system of a false positive federated (archive) file system.
 *
 * @author Christian Schlichtherle
 */
final class PersistentFalsePositiveArchiveException extends FalsePositiveArchiveException {

    private static final long serialVersionUID = 0;

    PersistentFalsePositiveArchiveException(IOException cause) {
        super(cause);
    }
}
