/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 */
//FIXME: @ServiceImplementation(priority = -100)
public final class PaceManagerDecorator implements FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return new PaceMediator().instrument(manager);
    }
}
