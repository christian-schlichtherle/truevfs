/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.FsModel;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system is a false positive file system.
 * <p>
 * This exception type is reserved for use by
 * {@link FsController file system controllers} in order to reroute file system
 * operations to the parent file system of a false positive federated (archive)
 * file system.
 *
 * @see    FsFalsePositiveController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
class FsFalsePositiveException extends FsControlFlowIOException {

    FsFalsePositiveException(FsModel model, IOException cause) {
        super(model, null, cause);
        assert null != cause;
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
