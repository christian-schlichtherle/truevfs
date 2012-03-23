/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JulOutputStream<E extends Entry>
extends DecoratingOutputStream {
    private static final Logger
            logger = Logger.getLogger(JulOutputStream.class.getName());

    private final Entry target;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JulOutputStream(final OutputSocket<?> socket) throws IOException {
        this(socket, socket.getLocalTarget());
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    private JulOutputStream(final OutputSocket<?> socket, final Entry target)
    throws IOException {
        super(socket.newOutputStream());
        this.target = target;
        Level level = target instanceof IOPool.Buffer ? Level.FINER : Level.FINEST;
        logger.log(level, "Stream writing " + target, new NeverThrowable());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            Level level = target instanceof IOPool.Buffer ? Level.FINER : Level.FINEST;
            logger.log(level, "Closed " + target, new NeverThrowable());
        }
    }
}