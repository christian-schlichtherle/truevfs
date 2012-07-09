/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jul;

import java.io.IOException;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.ext.inst.InstrumentingIoPool;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulIoPool<B extends IoBuffer<B>>
extends InstrumentingIoPool<B> {

    private static final Logger
            logger = Logger.getLogger(JulIoPool.class.getName());

    JulIoPool(JulDirector director, IoPool<B> model) {
        super(director, model);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new JulBuffer(pool.allocate());
    }

    private final class JulBuffer extends InstrumentingBuffer {
        JulBuffer(IoBuffer<B> model) {
            super(model);
            logger.log(FINE, "Allocated " + entry, new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            try {
                entry.release();
            } finally {
                logger.log(FINE, "Released " + entry, new NeverThrowable());
            }
        }
    } // JulBuffer
}