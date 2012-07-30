/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import net.java.truecommons.io.DecoratingSeekableChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogOutputChannel extends DecoratingSeekableChannel {
    private static final Logger
            logger = LoggerFactory.getLogger(LogOutputChannel.class);

    private final OutputSocket<?> socket;

    @CreatesObligation
    LogOutputChannel(
            final OutputSocket<? extends Entry> socket,
            final InputSocket<? extends Entry> peer)
    throws IOException {
        super(socket.channel(peer));
        this.socket = socket;
        log("Opened output channel for {}");
    }

    @Override
    public void close() throws IOException {
        channel.close();
        log("Closed output channel for {}");
    }

    private void log(String message) {
        Entry entry;
        try {
            entry = socket.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        logger.debug(message, entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new NeverThrowable());
    }
}
