/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.log;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.truevfs.comp.inst.InstrumentingIoBufferPool;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogIoBufferPool<B extends IoBuffer<B>>
extends InstrumentingIoBufferPool<B> {

    private static final Logger
            logger = LoggerFactory.getLogger(LogIoBufferPool.class);

    LogIoBufferPool(LogDirector director, IoBufferPool<B> model) {
        super(director, model);
    }

    @Override
    public IoBuffer<B> allocate() throws IOException {
        return new JulIoBuffer(pool.allocate());
    }

    private final class JulIoBuffer extends InstrumentingIoBuffer {
        JulIoBuffer(IoBuffer<B> model) {
            super(model);
            logger.debug("Allocated I/O buffer {}", entry);
            if (logger.isTraceEnabled())
                logger.trace("Stack trace:", new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            entry.release();
            logger.debug("Released I/O buffer {}", entry);
            if (logger.isTraceEnabled())
                logger.trace("Stack trace:", new NeverThrowable());
        }
    } // JulIoBuffer
}
