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
        log("Stream writing ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        out.close();
    }

    private void log(String message) {
        Entry entry;
        try {
            entry = socket.target();
        } catch (final IOException ignore) {
            entry = null;
        }
        if (entry instanceof IoBuffer<?>)
            logger.debug(message + entry, new NeverThrowable());
        else
            logger.trace(message + entry, new NeverThrowable());
    }
}
