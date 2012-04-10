/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulInputChannel extends DecoratingReadOnlyChannel {
    private static final Logger
            logger = Logger.getLogger(JulInputChannel.class.getName());

    private final InputSocket<?> socket;

    @CreatesObligation
    JulInputChannel(final InputSocket<?> socket) throws IOException {
        super(socket.channel());
        this.socket = socket;
        log("Random reading ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        channel.close();
    }

    private void log(String message) {
        Entry target;
        try {
            target = socket.getLocalTarget();
        } catch (final IOException ignore) {
            target = null;
        }
        final Level level = target instanceof IOBuffer
                ? Level.FINER
                : Level.FINEST;
        logger.log(level, message + target, new NeverThrowable());
    }
}
