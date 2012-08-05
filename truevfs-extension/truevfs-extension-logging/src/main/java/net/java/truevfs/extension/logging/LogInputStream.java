/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogInputStream extends DecoratingInputStream {
    private static final Logger
            logger = LoggerFactory.getLogger(LogInputStream.class);

    private final InputSocket<? extends Entry> origin;

    @CreatesObligation
    LogInputStream(
            final InputSocket<? extends Entry> origin,
            final InputStream in) {
        super(in);
        this.origin = origin;
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
            entry = origin.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        logger.debug(message, entry);
        if (logger.isTraceEnabled())
            logger.trace("Stack trace:", new Throwable());
    }
}
