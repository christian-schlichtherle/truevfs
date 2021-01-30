/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

import static net.java.truevfs.ext.insight.I5tMediators.syncOperationsMediator;

/**
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -200)
public final class I5tBufferPoolDecorator implements IoBufferPoolDecorator {

    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return syncOperationsMediator.instrument(pool);
    }
}
