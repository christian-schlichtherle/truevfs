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
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class JmxIOPool<E extends Entry<E>> extends InstrumentingIOPool<E> {

    JmxIOPool(IOPool<E> model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public Entry<E> allocate() throws IOException {
        return new JmxEntry(delegate.allocate());
    }

    private final class JmxEntry
    extends InstrumentingEntry {

        @SuppressWarnings("LeakingThisInConstructor")
        JmxEntry(Entry<E> model) {
            super(model);
            JmxEntryView.register(this);
        }

        @Override
        public void release() throws IOException {
            try {
                delegate.release();
            } finally {
                JmxEntryView.unregister(this);
            }
        }
    }
}
