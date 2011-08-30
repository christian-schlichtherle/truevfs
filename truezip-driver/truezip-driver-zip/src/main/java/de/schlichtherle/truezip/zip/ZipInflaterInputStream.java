/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.util.JSE7;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * An inflater input stream which uses a pooled {@link Inflater} and provides
 * access to it.
 * Inflaters are expensive to allocate, so pooling them improves performance.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class ZipInflaterInputStream extends InflaterInputStream {

    private static final InflaterCache cache = JSE7.AVAILABLE
                        ? new InflaterCache()        // JDK 7 is OK
                        : new Jdk6InflaterCache();   // JDK 6 needs fixing

    private boolean closed;

    ZipInflaterInputStream(DummyByteInputStream in, int size) {
        super(in, cache.allocate(), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        try {
            super.close();
        } finally {
            inf.reset();
            cache.release(inf);
        }
    }

    private static class InflaterCache extends CachedResourcePool<Inflater> {
        @Override
        protected Inflater newResource() {
            return new Inflater(true);
        }
    }

    private static final class Jdk6InflaterCache extends InflaterCache {
        @Override
        protected Inflater newResource() {
            return new Jdk6Inflater(true);
        }
    }
}
