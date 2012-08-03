/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import java.io.IOException;
import net.java.truevfs.component.instrumentation.InstrumentingIoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
final class LogIoBuffer<B extends IoBuffer<B>>
extends InstrumentingIoBuffer<LogDirector, B> {
    private static final Logger logger =
            LoggerFactory.getLogger(LogIoBufferPool.class);

    LogIoBuffer(LogDirector director, IoBuffer<B> model) {
        super(director, model);
        logger.debug("Allocated I/O buffer {}", entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new Throwable());
    }

    @Override
    public void release() throws IOException {
        entry.release();
        logger.debug("Released I/O buffer {}", entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new Throwable());
    }
}
