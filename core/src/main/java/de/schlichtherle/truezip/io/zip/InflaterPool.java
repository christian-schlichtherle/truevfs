/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.zip;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.Inflater;

/**
 * Provides utility methods for pooling {@link Inflater}s.
 * Inflater objects are expensive to allocate, so pooling them improves
 * performance.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
class InflaterPool {
    private static final Set<Inflater> allocated
            = new HashSet<Inflater>();
    private static final List<Reference<Inflater>> released
            = new LinkedList<Reference<Inflater>>();

    private InflaterPool() {
    }

    static Inflater fetch() {
        Inflater inflater = null;

        synchronized (released) {
            for (Iterator<Reference<Inflater>> i = released.iterator(); i.hasNext(); ) {
                inflater = i.next().get();
                i.remove();
                if (inflater != null) {
                    //inflater.reset();
                    break;
                }
            }
            if (inflater == null)
                inflater = new Inflater(true);

            // We MUST make sure that we keep a strong reference to the
            // inflater in order to retain it from being released again and
            // then finalized when the close() method of the InputStream
            // returned by getInputStream(...) is called from within another
            // finalizer.
            // The finalizer of the inflater calls end() and leaves the object
            // in a state so that the subsequent call to reset() throws an NPE.
            // The ZipFile class in Sun's JSE 5 shows this bug.
            allocated.add(inflater);
        }

        return inflater;
    }

    static void release(Inflater inflater) {
        inflater.reset();
        synchronized (released) {
            released.add(new SoftReference(inflater));
            allocated.remove(inflater);
        }
    }
}
