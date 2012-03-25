/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.schlichtherle.truezip.cio.IOBuffer;
import de.schlichtherle.truezip.cio.IOPool;
import de.truezip.extension.jmxjul.InstrumentingIOPool;
import java.io.IOException;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulIOPool<B extends IOBuffer<B>>
extends InstrumentingIOPool<B> {

    private static final Logger
            logger = Logger.getLogger(JulIOPool.class.getName());

    JulIOPool(IOPool<B> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public IOBuffer<B> allocate() throws IOException {
        return new JulBuffer(pool.allocate());
    }

    private final class JulBuffer extends InstrumentingBuffer {

        JulBuffer(IOBuffer<B> model) {
            super(model);
            logger.log(FINE, "Allocated " + delegate, new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            try {
                delegate.release();
            } finally {
                logger.log(FINE, "Released " + delegate, new NeverThrowable());
            }
        }
    } // JulBuffer
}