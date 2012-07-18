/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import java.io.IOException;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulIOPool<E extends Entry<E>>
extends InstrumentingIOPool<E, JulDirector> {

    private static final Logger
            logger = Logger.getLogger(JulIOPool.class.getName());

    JulIOPool(IOPool<E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public Entry<E> allocate() throws IOException {
        return new Buffer(delegate.allocate());
    }

    private final class Buffer
    extends InstrumentingIOPool<E, JulDirector>.Buffer {
        Buffer(Entry<E> model) {
            super(model);
            logger.log(FINE, "Allocated " + delegate, new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            delegate.release();
            logger.log(FINE, "Released " + delegate, new NeverThrowable());
        }
    } // Buffer
}