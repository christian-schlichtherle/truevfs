/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.cio.*;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <B> the type parameter for the I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIOPool<B extends IoBuffer<B>> implements IoPool<B> {

    protected final IoPool<B> pool;
    protected final InstrumentingDirector<?> director;

    public InstrumentingIOPool( final IoPool<B> pool,
                                final InstrumentingDirector<?> director) {
        this.pool = Objects.requireNonNull(pool);
        this.director = Objects.requireNonNull(director);
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