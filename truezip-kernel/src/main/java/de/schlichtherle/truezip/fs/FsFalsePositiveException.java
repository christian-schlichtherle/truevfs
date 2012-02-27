/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

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
public class FsFalsePositiveException extends FsControllerException {

    public FsFalsePositiveException(IOException cause) {
        super(cause);
        assert null != cause;
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
