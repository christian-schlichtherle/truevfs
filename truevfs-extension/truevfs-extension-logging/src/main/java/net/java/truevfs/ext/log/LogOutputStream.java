/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.log;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogOutputStream extends DecoratingOutputStream {
    private static final Logger
            logger = LoggerFactory.getLogger(LogOutputStream.class);

    private final OutputSocket<? extends Entry> origin;

    @CreatesObligation
    LogOutputStream(
            final OutputSocket<? extends Entry> origin,
            final OutputStream out) {
        super(out);
        this.origin = origin;
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
            entry = origin.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        logger.debug(message, entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new Throwable());
    }
}
