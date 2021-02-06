/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.api.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 */
//FIXME: @ServiceImplementation(priority = -300)
public final class LogBufferPoolDecorator implements IoBufferPoolDecorator {

    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return LogMediator.SINGLETON.instrument(pool);
    }
}
