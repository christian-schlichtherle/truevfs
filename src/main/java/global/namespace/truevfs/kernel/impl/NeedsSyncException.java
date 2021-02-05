/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.commons.shed.ControlFlowException;

/**
 * Indicates that a file system controller needs to get
 * {@linkplain global.namespace.truevfs.kernel.api.FsController#sync(BitField) synced} before the operation can get retried.
 *
 * @author Christian Schlichtherle
 * @see SyncController
 */
final class NeedsSyncException extends ControlFlowException {

    private static final long serialVersionUID = 0;
    private static final NeedsSyncException instance = new NeedsSyncException();

    private NeedsSyncException() {
        super(false);
    }

    static NeedsSyncException apply() {
        return isTraceable() ? new NeedsSyncException() : instance;
    }
}
