/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.sbc.DecoratingSeekableByteChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulOutputByteChannel extends DecoratingSeekableByteChannel {
    private static final Logger
            logger = Logger.getLogger(JulOutputByteChannel.class.getName());

    private final OutputSocket<?> socket;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JulOutputByteChannel(final OutputSocket<?> socket) throws IOException {
        super(socket.newChannel());
        this.socket = socket;
        log("Random writing ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        sbc.close();
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
