/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.ControlFlowException;

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
