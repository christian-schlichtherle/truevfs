/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingIoPool;
import net.truevfs.kernel.cio.IoBuffer;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIoPool<B extends IoBuffer<B>>
extends InstrumentingIoPool<B> {

    JmxIoPool(IoPool<B> model, JmxDirector director) {
        super(director, model);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new JmxBuffer(pool.allocate());
    }

    private final class JmxBuffer extends InstrumentingBuffer {

        @SuppressWarnings("LeakingThisInConstructor")
        JmxBuffer(IoBuffer<B> model) {
            super(model);
            JmxIoBufferView.register(this);
        }

        @Override
        public void release() throws IOException {
            try {
                entry.release();
            } finally {
                JmxIoBufferView.unregister(this);
            }
        }
    } // JmxBuffer
}