/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class LogIoBufferPoolDecorator extends IoBufferPoolDecorator {
    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return LogDirector.SINGLETON.instrument(pool);
    }

    /** Returns -100. */
    @Override
    public int getPriority() {
        return -100;
    }
}
