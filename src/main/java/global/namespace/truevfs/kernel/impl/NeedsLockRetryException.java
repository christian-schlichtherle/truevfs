/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.commons.shed.ControlFlowException;

/**
 * Indicates that all file system locks need to get released before the operation can get retried.
 *
 * @author Christian Schlichtherle
 * @see LockController
 */
final class NeedsLockRetryException extends ControlFlowException {

    private static final long serialVersionUID = 0;
    private static final NeedsLockRetryException instance = new NeedsLockRetryException();

    private NeedsLockRetryException() {
        super(false);
    }

    static NeedsLockRetryException apply() {
        return isTraceable() ? new NeedsLockRetryException() : instance;
    }
}
