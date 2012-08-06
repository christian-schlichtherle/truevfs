/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JmxIoBufferPoolDecorator extends IoBufferPoolDecorator {
    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return JmxDirector.SINGLETON.instrument(pool);
    }
}
