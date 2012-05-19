/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.IOPool;
import de.truezip.extension.jmxjul.InstrumentingIOPool;
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
                entry.release();
            } finally {
                JmxIOBufferView.unregister(this);
            }
        }
    } // JmxBuffer
}