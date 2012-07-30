/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingIoBufferPool;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIoBufferPool<B extends IoBuffer<B>>
extends InstrumentingIoBufferPool<B> {

    JmxIoBufferPool(JmxDirector director, IoBufferPool<B> model) {
        super(director, model);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new JmxBuffer(pool.allocate());
    }

    private final class JmxBuffer extends InstrumentingIoBuffer {

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