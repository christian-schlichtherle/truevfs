/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jul;

import java.io.IOException;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.ext.inst.InstrumentingIoBufferPool;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulIoBufferPool<B extends IoBuffer<B>>
extends InstrumentingIoBufferPool<B> {

    private static final Logger
            logger = Logger.getLogger(JulIoBufferPool.class.getName());

    JulIoBufferPool(JulDirector director, IoBufferPool<B> model) {
        super(director, model);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new JulIoBuffer(pool.allocate());
    }

    private final class JulIoBuffer extends InstrumentingIoBuffer {
        JulIoBuffer(IoBuffer<B> model) {
            super(model);
            logger.log(FINE, "Allocated " + entry, new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            entry.release();
            logger.log(FINE, "Released " + entry, new NeverThrowable());
        }
    } // JulIoBuffer
}