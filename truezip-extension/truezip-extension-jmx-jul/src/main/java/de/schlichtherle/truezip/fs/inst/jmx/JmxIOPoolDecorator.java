/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.spi.IOPoolDecorator;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JmxIOPoolDecorator extends IOPoolDecorator {
    @Override
    public <B extends IOPool.Entry<B>> IOPool<B> decorate(IOPool<B> pool) {
        return JmxDirector.SINGLETON.instrument(pool);
    }

    /** Returns 100. */
    @Override
    public int getPriority() {
        return 100;
    }
}
