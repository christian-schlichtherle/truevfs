/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -300)
public final class LogBufferPoolDecorator implements IoBufferPoolDecorator {

    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return LogMediator.SINGLETON.instrument(pool);
    }
}
