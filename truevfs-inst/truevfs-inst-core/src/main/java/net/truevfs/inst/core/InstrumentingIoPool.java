/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.inst.core;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.*;

/**
 * @param  <B> the type parameter for the I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIoPool<B extends IoBuffer<B>> implements IoPool<B> {

    protected final InstrumentingDirector<?> director;
    protected final IoPool<B> pool;

    public InstrumentingIoPool(
            final InstrumentingDirector<?> director,
            final IoPool<B> pool) {
        this.director = Objects.requireNonNull(director);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new InstrumentingBuffer(pool.allocate());
    }

    @Override
    public void release(IoBuffer<B> resource) throws IOException {
        resource.release();
    }

    @SuppressWarnings("PublicInnerClass")
    public class InstrumentingBuffer
    extends DecoratingEntry<IoBuffer<B>>
    implements IoBuffer<B> {

        protected InstrumentingBuffer(IoBuffer<B> buffer) {
            super(buffer);
        }

        @Override
        public InputSocket<B> input() {
            return director.instrument(entry.input(), this);
        }

        @Override
        public OutputSocket<B> output() {
            return director.instrument(entry.output(), this);
        }

        @Override
        public void release() throws IOException {
            entry.release();
        }
    } // InstrumentingBuffer
}
