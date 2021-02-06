/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.spi.FsManagerDecorator;

import static global.namespace.truevfs.ext.insight.I5tMediators.syncOperationsMediator;

/**
 * @author Christian Schlichtherle
 */
//FIXME: @ServiceImplementation(priority = -200)
public final class I5tManagerDecorator implements FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return syncOperationsMediator.instrument(manager);
    }
}
