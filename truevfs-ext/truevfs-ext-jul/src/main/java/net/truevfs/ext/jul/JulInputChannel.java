/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jul;

import de.schlichtherle.truecommons.io.ReadOnlyChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulInputChannel extends ReadOnlyChannel {
    private static final Logger
            logger = Logger.getLogger(JulInputChannel.class.getName());

    private final InputSocket<?> socket;

    @CreatesObligation
    JulInputChannel(
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
        Entry target;
        try {
            target = socket.target();
        } catch (final IOException ignore) {
            target = null;
        }
        final Level level = target instanceof IoBuffer
                ? Level.FINER
                : Level.FINEST;
        logger.log(level, message + target, new NeverThrowable());
    }
}