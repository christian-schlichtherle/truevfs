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
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class JulOutputStream<E extends Entry>
extends DecoratingOutputStream {
    private static final Logger
            logger = Logger.getLogger(JulOutputStream.class.getName());

    private final JulOutputSocket<E> socket;

    JulOutputStream(final OutputStream model, final JulOutputSocket<E> socket)
    throws IOException {
        super(model);
        if (null == model)
            throw new NullPointerException();
        this.socket = socket;
        E target = socket.getLocalTarget();
        Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
        logger.log(level, "Stream writing " + target, new NeverThrowable());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            E target = socket.getLocalTarget();
            Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
            logger.log(level, "Closed " + target, new NeverThrowable());
        }
    }
    
}
