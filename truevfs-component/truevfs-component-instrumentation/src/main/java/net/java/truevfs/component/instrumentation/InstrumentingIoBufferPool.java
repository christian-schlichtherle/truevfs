/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.AbstractIoBufferPool;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIoBufferPool<D extends Director<D>>
extends AbstractIoBufferPool {
    protected final D director;
    protected final IoBufferPool pool;

    public InstrumentingIoBufferPool(
            final D director,
            final IoBufferPool pool) {
        this.director = Objects.requireNonNull(director);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer allocate() throws IOException {
        return director.instrument(this, pool.allocate());
    }

    @Override
    public String toString() {
        return String.format("%s[pool=%s]", getClass().getName(), pool);
    }
}
