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
final class LogIoBuffer extends InstrumentingIoBuffer<LogDirector> {
    private static final Logger logger =
            LoggerFactory.getLogger(LogIoBuffer.class);

    LogIoBuffer(LogDirector director, IoBuffer buffer) {
        super(director, buffer);
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
