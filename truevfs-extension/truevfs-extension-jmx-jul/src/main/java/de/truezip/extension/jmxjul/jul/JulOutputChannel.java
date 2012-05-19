/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulOutputChannel extends DecoratingSeekableChannel {
    private static final Logger
            logger = Logger.getLogger(JulOutputChannel.class.getName());

    private final OutputSocket<?> socket;

    @CreatesObligation
    JulOutputChannel(final OutputSocket<?> socket) throws IOException {
        super(socket.channel());
        this.socket = socket;
        log("Random writing ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        channel.close();
    }

    private void log(String message) {
        Entry target;
        try {
            target = socket.localTarget();
        } catch (final IOException ignore) {
            target = null;
        }
        final Level level = target instanceof IOBuffer
                ? Level.FINER
                : Level.FINEST;
        logger.log(level, message + target, new NeverThrowable());
    }
}
