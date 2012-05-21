/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import net.truevfs.extension.jmxjul.InstrumentingIOPool;
import net.truevfs.kernel.cio.IoBuffer;
import net.truevfs.kernel.cio.IoPool;
import java.io.IOException;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulIOPool<B extends IoBuffer<B>>
extends InstrumentingIOPool<B> {

    private static final Logger
            logger = Logger.getLogger(JulIOPool.class.getName());

    JulIOPool(IoPool<B> model, JulDirector director) {
        super(model, director);
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