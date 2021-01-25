/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

import static net.java.truevfs.ext.insight.I5tMediators.syncOperationsMediator;

/**
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the
 * {@link net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator} singleton!
 */
@Deprecated
public final class I5tBufferPoolDecorator extends IoBufferPoolDecorator {

    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return syncOperationsMediator.instrument(pool);
    }

    /**
     * Returns {@code -200}.
     */
    @Override
    public int getPriority() {
        return -200;
    }
}
