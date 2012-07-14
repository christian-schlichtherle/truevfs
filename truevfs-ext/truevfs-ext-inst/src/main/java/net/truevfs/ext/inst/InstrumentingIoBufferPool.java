/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.inst;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.*;

/**
 * @param  <B> the type of I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIoBufferPool<B extends IoBuffer<B>>
implements IoBufferPool<B> {

    protected final InstrumentingDirector<?> director;
    protected final IoBufferPool<B> pool;

    public InstrumentingIoBufferPool(
            final InstrumentingDirector<?> director,
            final IoBufferPool<B> pool) {
        this.director = Objects.requireNonNull(director);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new InstrumentingIoBuffer(pool.allocate());
    }

    @Override
    public void release(IoBuffer<B> resource) throws IOException {
        resource.release();
    }

    @Override
    public String toString() {
        return String.format("%s[pool=%s]", getClass().getName(), pool);
    }

    @SuppressWarnings("PublicInnerClass")
    public class InstrumentingIoBuffer
    extends DecoratingEntry<IoBuffer<B>>
    implements IoBuffer<B> {

        protected InstrumentingIoBuffer(IoBuffer<B> buffer) {
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
    } // InstrumentingIoBuffer
}
