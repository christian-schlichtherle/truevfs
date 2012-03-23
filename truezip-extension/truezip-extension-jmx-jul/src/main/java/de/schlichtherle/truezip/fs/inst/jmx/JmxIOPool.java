/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.entry.IOPool;
import de.schlichtherle.truezip.entry.IOPool.IOBuffer;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIOPool<E extends IOBuffer<E>>
extends InstrumentingIOPool<E, JmxDirector> {

    JmxIOPool(IOPool<E> model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public IOBuffer<E> allocate() throws IOException {
        return new JmxBuffer(delegate.allocate());
    }

    private final class JmxBuffer
    extends InstrumentingIOPool<E, JmxDirector>.InstrumentingBuffer {

        @SuppressWarnings("LeakingThisInConstructor")
        JmxBuffer(IOBuffer<E> model) {
            super(model);
            JmxIOBufferView.register(this);
        }

        @Override
        public void release() throws IOException {
            try {
                delegate.release();
            } finally {
                JmxIOBufferView.unregister(this);
            }
        }
    } // IOBuffer
}