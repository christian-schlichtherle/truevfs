/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JulReadOnlyFile extends DecoratingReadOnlyFile {
    private static final Logger
            logger = Logger.getLogger(JulReadOnlyFile.class.getName());

    private final InputSocket<?> socket;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JulReadOnlyFile(final InputSocket<?> socket) throws IOException {
        super(socket.newReadOnlyFile());
        this.socket = socket;
        log("Random reading ");
    }

    @Override
    public void close() throws IOException {
        log("Closing ");
        delegate.close();
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
