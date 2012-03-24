/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.entry.IOBuffer;
import de.schlichtherle.truezip.entry.IOPool;
import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIOPool<B extends IOBuffer<B>>
extends InstrumentingIOPool<B> {

    JmxIOPool(IOPool<B> model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public IOBuffer<B> allocate() throws IOException {
        return new JmxBuffer(pool.allocate());
    }

    private final class JmxBuffer extends InstrumentingBuffer {

        @SuppressWarnings("LeakingThisInConstructor")
        JmxBuffer(IOBuffer<B> model) {
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
    } // JmxBuffer
}