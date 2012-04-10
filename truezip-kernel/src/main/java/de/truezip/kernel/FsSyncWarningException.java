/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * An exception of this class implies that no or only insignificant parts
 * of the data of the federated file system has been lost.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsSyncWarningException extends FsSyncException {

    private static final long serialVersionUID = 2302357394858347366L;

    public FsSyncWarningException(FsModel model, IOException cause) {
        super(model, cause);
    }

    /** @return {@code -10}. */
    @Override
    public int getPriority() {
        return -10;
    }
}
