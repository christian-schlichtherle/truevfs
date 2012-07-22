/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.log;

import de.schlichtherle.truecommons.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class LogOutputStream extends DecoratingOutputStream {
    private static final Logger
            logger = LoggerFactory.getLogger(LogOutputStream.class);

    private final OutputSocket<?> socket;

    @CreatesObligation
    LogOutputStream(
            final OutputSocket<? extends Entry> socket,
            final InputSocket<? extends Entry> peer)
    throws IOException {
        super(socket.stream(peer));
        this.socket = socket;
        log("Opened output stream for {}");
    }

    @Override
    public void close() throws IOException {
        out.close();
        log("Closed output stream for {}");
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
