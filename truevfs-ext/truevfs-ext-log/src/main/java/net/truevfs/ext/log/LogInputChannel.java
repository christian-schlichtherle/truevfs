/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.log;

import de.schlichtherle.truecommons.io.ReadOnlyChannel;
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
        log("Random reading ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        channel.close();
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
