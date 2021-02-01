/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.IoSocket;
import global.namespace.truevfs.comp.io.ReadOnlyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SeekableByteChannel;

final class LogInputChannel extends ReadOnlyChannel implements LogCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LogInputChannel.class);

    private final IoSocket<? extends Entry> context;

    LogInputChannel(final IoSocket<? extends Entry> context, final SeekableByteChannel channel) {
        super(channel);
        this.context = context;
        opening();
    }

    @Override
    public IoSocket<? extends Entry> context() {
        return context;
    }

    @Override
    public Logger logger() {
        return logger;
    }
}
