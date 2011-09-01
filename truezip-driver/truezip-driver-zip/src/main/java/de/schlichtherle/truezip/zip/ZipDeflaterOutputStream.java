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

    private boolean released;

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, cache.allocate(), size);
        def.setLevel(level);
    }

    Deflater getDeflater() {
        return def;
    }

    void releaseDeflater() throws IOException {
        if (released)
            return;
        released = true;
        cache.release(def);
    }

    @Override
    public void close() throws IOException {
        assert false;
        try {
            super.close();
        } finally {
            releaseDeflater();
        }
    }

    private static class DeflaterCache
    extends CachedResourcePool<Deflater, RuntimeException> {
        @Override
        protected Deflater newResource() {
            return new Deflater(DEFAULT_COMPRESSION, true);
        }

        @Override
        protected void reset(Deflater def) {
            def.reset();
        }
    }

    private static final class Jdk6DeflaterCache extends DeflaterCache {
        @Override
        protected Deflater newResource() {
            return new Jdk6Deflater(DEFAULT_COMPRESSION, true);
        }
    }
}
