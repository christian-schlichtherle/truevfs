/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import net.java.truecommons.cio.IoBuffer;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

final class LogBuffer extends InstrumentingBuffer<LogMediator> implements LogResource {

    private static final Logger logger = LoggerFactory.getLogger(LogBuffer.class);

    LogBuffer(LogMediator mediator, IoBuffer entry) {
        super(mediator, entry);
        log("Allocated {}", entry);
    }

    @Override
    public void release() throws IOException {
        log("Releasing {}", entry);
        entry.release();
    }

    @Override
    public Logger logger() {
        return logger;
    }
}
