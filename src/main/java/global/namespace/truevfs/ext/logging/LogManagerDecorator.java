/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.kernel.spec.FsManager;
import global.namespace.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -300)
public final class LogManagerDecorator implements FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return LogMediator.SINGLETON.instrument(manager);
    }
}
