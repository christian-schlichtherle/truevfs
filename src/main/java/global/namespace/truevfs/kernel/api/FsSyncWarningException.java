/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import java.io.IOException;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * An exception of this class implies that no or only insignificant parts
 * of the data in the federated file system has been lost.
 *
 * @author Christian Schlichtherle
 */
public class FsSyncWarningException extends FsSyncException {

    private static final long serialVersionUID = 0;

    public FsSyncWarningException(FsMountPoint mountPoint, IOException cause) {
        super(mountPoint, cause);
    }

    /** @return {@code -10}. */
    @Override public int getPriority() { return -10; }
}
