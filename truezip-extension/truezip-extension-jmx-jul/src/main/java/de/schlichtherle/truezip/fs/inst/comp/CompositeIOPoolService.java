/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.inst.comp;

import de.schlichtherle.truezip.fs.inst.jmx.JmxDirector;
import de.schlichtherle.truezip.fs.inst.jul.JulDirector;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.spi.IOPoolService;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
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
