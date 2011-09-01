/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private static final InflaterFactory factory = JSE7.AVAILABLE
                        ? new InflaterFactory()        // JDK 7 is OK
                        : new Jdk6InflaterFactory();   // JDK 6 needs fixing

    private boolean closed;

    ZipInflaterInputStream(DummyByteInputStream in, int size) {
        super(in, factory.newInflater(), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        super.close();
    }

    private static class InflaterFactory {
        protected Inflater newInflater() {
            return new Inflater(true);
        }
    }

    private static final class Jdk6InflaterFactory extends InflaterFactory {
        @Override
        protected Inflater newInflater() {
            return new Jdk6Inflater(true);
        }
    }
}
