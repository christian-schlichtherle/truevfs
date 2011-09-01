/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.util.CachedResourcePool;
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
        inf.reset();
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
            cache.release(inf);
        }
    }

    private static class InflaterCache
    extends CachedResourcePool<Inflater, RuntimeException> {
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
