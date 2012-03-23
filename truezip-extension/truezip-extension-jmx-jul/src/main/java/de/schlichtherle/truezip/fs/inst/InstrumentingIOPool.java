/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Buffer;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <E> the type of the I/O buffers.
 * @param  <D> the type of the instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingIOPool<
        E extends Buffer<E>,
        D extends InstrumentingDirector<D>>
implements IOPool<E> {

    protected final D director;
    protected final IOPool<E> delegate;

    public InstrumentingIOPool(final IOPool<E> pool, final D director) {
        if (null == pool || null == director)
            throw new NullPointerException();
        this.director = director;
        this.delegate = pool;
    }

    @Override
    public Buffer<E> allocate() throws IOException {
        return new InstrumentingBuffer(delegate.allocate());
    }

    @Override
    public void release(Buffer<E> resource) throws IOException {
        resource.release();
    }

    @SuppressWarnings("PublicInnerClass")
    public class InstrumentingBuffer
    extends DecoratingEntry<Buffer<E>>
    implements Buffer<E> {

        protected InstrumentingBuffer(Buffer<E> delegate) {
            super(delegate);
        }

        @Override
        public InputSocket<E> getInputSocket() {
            return director.instrument(delegate.getInputSocket(), this);
        }

        @Override
        public OutputSocket<E> getOutputSocket() {
            return director.instrument(delegate.getOutputSocket(), this);
        }

        @Override
        public void release() throws IOException {
            delegate.release();
        }
    } // Buffer
}