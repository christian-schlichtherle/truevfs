/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.truevfs.commons.cio.IoBuffer;
import global.namespace.truevfs.commons.inst.InstrumentingBuffer;
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
