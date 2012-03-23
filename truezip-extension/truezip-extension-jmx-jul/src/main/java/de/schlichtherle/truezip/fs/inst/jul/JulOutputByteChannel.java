/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.entry.IOPool;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JulOutputByteChannel<E extends Entry>
extends DecoratingSeekableByteChannel {
    private static final Logger
            logger = Logger.getLogger(JulOutputByteChannel.class.getName());

    private final JulOutputSocket<E> socket;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JulOutputByteChannel(
            final @WillCloseWhenClosed SeekableByteChannel model,
            final JulNio2OutputSocket<E> socket)
    throws IOException {
        super(model);
        if (null == model)
            throw new NullPointerException();
        this.socket = socket;
        E target = socket.getLocalTarget();
        Level level = target instanceof IOPool.IOBuffer ? Level.FINER : Level.FINEST;
        logger.log(level, "Randomly writing " + target, new NeverThrowable());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            E target = socket.getLocalTarget();
            Level level = target instanceof IOPool.IOBuffer ? Level.FINER : Level.FINEST;
            logger.log(level, "Closed " + target, new NeverThrowable());
        }
    }
}