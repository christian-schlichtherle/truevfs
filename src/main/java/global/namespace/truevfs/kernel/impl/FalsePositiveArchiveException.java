/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.shed.ControlFlowException;

import java.io.IOException;

/**
 * Indicates that a file system is a false positive file system.
 * <p>
 * This exception type is reserved for non-local control flow in file system controller chains in order to reroute file
 * system operations to the parent file system of a false positive federated (archive) file system.
 *
 * @author Christian Schlichtherle
 * @see FalsePositiveArchiveController
 */
class FalsePositiveArchiveException extends ControlFlowException {

    private static final long serialVersionUID = 0;

    FalsePositiveArchiveException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
