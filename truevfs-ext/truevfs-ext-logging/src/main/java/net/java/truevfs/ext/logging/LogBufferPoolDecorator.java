/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the
 * {@link net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator} singleton!
 */
@Deprecated(since = "1")
public final class LogBufferPoolDecorator extends IoBufferPoolDecorator {

    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return LogMediator.SINGLETON.instrument(pool);
    }

    /**
     * Returns {@code -300}.
     */
    @Override
    public int getPriority() {
        return -300;
    }
}
