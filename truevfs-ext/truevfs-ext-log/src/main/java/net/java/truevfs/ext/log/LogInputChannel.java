/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.log;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.ReadOnlyChannel;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogInputChannel extends ReadOnlyChannel {
    private static final Logger
            logger = LoggerFactory.getLogger(LogInputChannel.class);

    private final InputSocket<? extends Entry> origin;

    @CreatesObligation
    LogInputChannel(
            final InputSocket<? extends Entry> origin,
            final SeekableByteChannel channel) {
        super(channel);
        this.origin = origin;
        log("Opened input channel for {}");
    }

    @Override
    public void close() throws IOException {
        channel.close();
        log("Closed input channel for {}");
    }

    private void log(String message) {
        Entry entry;
        try {
            entry = origin.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        logger.debug(message, entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new Throwable());
    }
}
