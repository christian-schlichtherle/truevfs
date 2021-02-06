/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.util.ControlFlowException;

/**
 * Indicates that an operation needs to get write locked before it can get retried.
 *
 * @author Christian Schlichtherle
 */
final class NeedsWriteLockException extends ControlFlowException {

    private static final long serialVersionUID = 0;
    private static final NeedsWriteLockException instance = new NeedsWriteLockException();

    private NeedsWriteLockException() {
        super(false);
    }

    static NeedsWriteLockException apply() {
        return isTraceable() ? new NeedsWriteLockException() : instance;
    }
}
