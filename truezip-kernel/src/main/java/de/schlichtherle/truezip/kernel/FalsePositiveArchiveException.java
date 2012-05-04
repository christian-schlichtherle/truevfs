/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsDecoratingController;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system is a false positive file system.
 * <p>
 * This exception type is reserved for non-local control flow in
 * {@linkplain FsDecoratingController file system controller chains} in order
 * to reroute file system operations to the parent file system of a false
 * positive federated (archive) file system.
 *
 * @see    FalsePositiveController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing a control flow exception is nonsense!
class FalsePositiveArchiveException extends ControlFlowException {

    FalsePositiveArchiveException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
