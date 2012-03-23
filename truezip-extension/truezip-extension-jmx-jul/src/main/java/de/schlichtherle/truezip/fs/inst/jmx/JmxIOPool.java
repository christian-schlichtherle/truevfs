/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Buffer;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIOPool<E extends Buffer<E>>
extends InstrumentingIOPool<E, JmxDirector> {

    JmxIOPool(IOPool<E> model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public Buffer<E> allocate() throws IOException {
        return new JmxBuffer(delegate.allocate());
    }

    private final class JmxBuffer
    extends InstrumentingIOPool<E, JmxDirector>.InstrumentingBuffer {

        @SuppressWarnings("LeakingThisInConstructor")
        JmxBuffer(Buffer<E> model) {
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
    } // Buffer
}