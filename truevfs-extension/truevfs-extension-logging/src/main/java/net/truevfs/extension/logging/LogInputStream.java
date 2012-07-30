/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.logging;

import net.java.truecommons.io.DecoratingInputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.OutputSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogInputStream extends DecoratingInputStream {
    private static final Logger
            logger = LoggerFactory.getLogger(LogInputStream.class);

    private final InputSocket<?> socket;

    @CreatesObligation
    LogInputStream(
            final InputSocket<? extends Entry> socket,
            final OutputSocket<? extends Entry> peer)
    throws IOException {
        super(socket.stream(peer));
        this.socket = socket;
        log("Opened input stream for {}");
    }

    @Override
    public void close() throws IOException {
        in.close();
        log("Closed input stream for {}");
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
