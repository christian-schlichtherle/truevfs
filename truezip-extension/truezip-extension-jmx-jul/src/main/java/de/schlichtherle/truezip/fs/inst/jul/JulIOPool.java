/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class JulIOPool<E extends Entry<E>> extends InstrumentingIOPool<E> {

    private static final Logger
            logger = Logger.getLogger(JulIOPool.class.getName());

    JulIOPool(IOPool<E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public Entry<E> allocate() throws IOException {
        return new LogEntry(delegate.allocate());
    }

    private final class LogEntry
    extends InstrumentingEntry {

        LogEntry(Entry<E> model) {
            super(model);
            logger.log(FINE, "Allocated " + delegate, new NeverThrowable());
        }

        @Override
        public void release() throws IOException {
            try {
                delegate.release();
            } finally {
                logger.log(FINE, "Released " + delegate, new NeverThrowable());
            }
        }
    }
}
