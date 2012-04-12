/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.cio.*;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <B> the type parameter for the I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIOPool<B extends IOBuffer<B>> implements IOPool<B> {

    protected final IOPool<B> pool;
    protected final InstrumentingDirector<?> director;

    public InstrumentingIOPool( final IOPool<B> pool,
                                final InstrumentingDirector<?> director) {
        if (null == (this.pool = pool))
            throw new NullPointerException();
        if (null == (this.director = director))
            throw new NullPointerException();
    }

    @Override
    public IOBuffer<B> allocate() throws IOException {
        return new InstrumentingBuffer(pool.allocate());
    }

    @Override
    public void release(IOBuffer<B> resource) throws IOException {
        resource.release();
    }

    @SuppressWarnings("PublicInnerClass")
    public class InstrumentingBuffer
    extends DecoratingEntry<IOBuffer<B>>
    implements IOBuffer<B> {

        protected InstrumentingBuffer(IOBuffer<B> buffer) {
            super(buffer);
        }

        @Override
        public InputSocket<B> inputSocket() {
            return director.instrument(entry.inputSocket(), this);
        }

        @Override
        public OutputSocket<B> outputSocket() {
            return director.instrument(entry.outputSocket(), this);
        }

        @Override
        public void release() throws IOException {
            entry.release();
        }
    } // InstrumentingBuffer
}