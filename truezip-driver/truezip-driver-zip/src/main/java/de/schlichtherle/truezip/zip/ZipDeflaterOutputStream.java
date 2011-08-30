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
import java.io.OutputStream;
import java.util.zip.Deflater;
import static java.util.zip.Deflater.*;
import java.util.zip.DeflaterOutputStream;

/**
 * A deflater output stream which uses a pooled {@link Deflater} and provides
 * access to it.
 * Deflaters are expensive to allocate, so pooling them improves performance.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class ZipDeflaterOutputStream extends DeflaterOutputStream {

    private static final DeflaterCache cache = JSE7.AVAILABLE
                        ? new DeflaterCache()        // JDK 7 is OK
                        : new Jdk6DeflaterCache();   // JDK 6 needs fixing

    private boolean reset;

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, cache.allocate(), size);
        def.setLevel(level);
    }

    Deflater getDeflater() {
        return def;
    }

    void resetDeflater() throws IOException {
        if (reset)
            return;
        reset = true;
        def.reset();
        cache.release(def);
    }

    @Override
    public void close() throws IOException {
        assert false;
        try {
            super.close();
        } finally {
            resetDeflater();
        }
    }

    private static class DeflaterCache extends CachedResourcePool<Deflater> {
        @Override
        protected Deflater newResource() {
            return new Deflater(DEFAULT_COMPRESSION, true);
        }
    }

    private static final class Jdk6DeflaterCache extends DeflaterCache {
        @Override
        protected Deflater newResource() {
            return new Jdk6Deflater(DEFAULT_COMPRESSION, true);
        }
    }
}
