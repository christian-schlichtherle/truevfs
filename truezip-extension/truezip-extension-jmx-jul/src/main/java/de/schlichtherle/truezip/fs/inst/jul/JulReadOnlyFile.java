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
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class JulReadOnlyFile<E extends Entry>
extends DecoratingReadOnlyFile {
    private static final Logger
            logger = Logger.getLogger(JulReadOnlyFile.class.getName());

    private final Entry target;

    JulReadOnlyFile(final InputSocket<?> socket) throws IOException {
        this(socket, socket.getLocalTarget());
    }

    private JulReadOnlyFile(final InputSocket<?> socket, final Entry target)
    throws IOException {
        super(socket.newReadOnlyFile());
        this.target = target;
        Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
        logger.log(level, "Randomly reading " + target, new NeverThrowable());
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
