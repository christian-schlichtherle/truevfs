/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import net.java.truecommons.io.ReadOnlyChannel;
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
final class LogInputChannel extends ReadOnlyChannel {
    private static final Logger
            logger = LoggerFactory.getLogger(LogInputChannel.class);

    private final InputSocket<?> socket;

    @CreatesObligation
    LogInputChannel(
            final InputSocket<? extends Entry> socket,
            final OutputSocket<? extends Entry> peer)
    throws IOException {
        super(socket.channel(peer));
        this.socket = socket;
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
            entry = socket.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        logger.debug(message, entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new NeverThrowable());
    }
}
