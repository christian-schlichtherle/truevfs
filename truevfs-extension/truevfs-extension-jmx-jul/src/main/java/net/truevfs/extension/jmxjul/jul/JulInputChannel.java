/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.IoBuffer;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.io.DecoratingReadOnlyChannel;
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
            target = socket.localTarget();
        } catch (final IOException ignore) {
            target = null;
        }
        final Level level = target instanceof IoBuffer
                ? Level.FINER
                : Level.FINEST;
        logger.log(level, message + target, new NeverThrowable());
    }
}
