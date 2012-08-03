/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @param  <D> the type of the instrumenting director.
 * @param  <B> the type of the instrumented I/O buffers.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIoBufferPool<
        D extends InstrumentingDirector<D>,
        B extends IoBuffer<B>>
implements IoBufferPool<B> {
    protected final D director;
    protected final IoBufferPool<B> pool;

    public InstrumentingIoBufferPool(
            final D director,
            final IoBufferPool<B> pool) {
        this.director = Objects.requireNonNull(director);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new InstrumentingIoBuffer<>(director, pool.allocate());
    }

    @Override
    public void release(IoBuffer<B> resource) throws IOException {
        resource.release();
    }

    @Override
    public String toString() {
        return String.format("%s[pool=%s]", getClass().getName(), pool);
    }
}
