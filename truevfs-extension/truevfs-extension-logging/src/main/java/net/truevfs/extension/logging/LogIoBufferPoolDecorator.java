/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.logging;

import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class LogIoBufferPoolDecorator extends IoBufferPoolDecorator {
    @Override
    public IoBufferPool<? extends IoBuffer<?>> apply(IoBufferPool<? extends IoBuffer<?>> pool) {
        return LogDirector.SINGLETON.instrument(pool);
    }

    /** Returns -100. */
    @Override
    public int getPriority() {
        return -100;
    }
}
