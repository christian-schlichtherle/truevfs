/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class JulInputStream<E extends Entry>
extends DecoratingInputStream {
    private static final Logger
            logger = Logger.getLogger(JulInputStream.class.getName());

    private final Entry target;

    JulInputStream(final InputSocket<?> socket) throws IOException {
        this(socket, socket.getLocalTarget());
    }

    private JulInputStream(final InputSocket<?> socket, final Entry target)
    throws IOException {
        super(socket.newInputStream());
        this.target = target;
        Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
        logger.log(level, "Stream reading " + target, new NeverThrowable());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
            logger.log(level, "Closed " + target, new NeverThrowable());
        }
    }
}
