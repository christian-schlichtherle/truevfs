/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.comp;

import de.schlichtherle.truezip.fs.inst.jmx.JmxDirector;
import de.schlichtherle.truezip.fs.inst.jul.JulDirector;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.spi.IOPoolService;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class CompositeIOPoolService extends IOPoolService {

    private static final IOPoolService SERVICE;
    static {
        IOPoolService oio = new de.schlichtherle.truezip.fs.file.TempFilePoolService();
        IOPoolService nio = new de.schlichtherle.truezip.fs.nio.file.TempFilePoolService();
        SERVICE = oio.getPriority() > nio.getPriority() ? oio : nio;
    }

    @SuppressWarnings("unchecked")
    private static final IOPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IOPool) SERVICE.get()));

    @Override
    public IOPool<?> get() {
        return pool;
    }

    /**
     * Returns 1 iff the JVM is running JSE 6 or 151 iff the JVM is running
     * JSE 7.
     * 
     * @return 1 iff the JVM is running JSE 6 or 151 iff the JVM is running
     *         JSE 7.
     */
    @Override
    public int getPriority() {
        return SERVICE.getPriority() * 3 / 2 + 1;
    }
}
