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
 * Unless this is an instance of the sub-class {@link FsSyncWarningException},
 * an exception of this class implies that some or all
 * of the data in the federated file system has been lost.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsSyncException extends IOException {
    private static final long serialVersionUID = 4893219420357369739L;

    public FsSyncException(FsModel model, IOException cause) {
        super(model.getMountPoint().toString(), cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }

    /** @return {@code 0}. */
    public int getPriority() {
        return 0;
    }
}
