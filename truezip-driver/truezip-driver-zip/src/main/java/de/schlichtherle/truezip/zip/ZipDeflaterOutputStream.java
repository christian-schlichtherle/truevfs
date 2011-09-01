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

    private static final DeflaterFactory factory = JSE7.AVAILABLE
                        ? new DeflaterFactory()        // JDK 7 is OK
                        : new Jdk6DeflaterFactory();   // JDK 6 needs fixing

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, factory.newDeflater(), size);
        def.setLevel(level);
    }

    Deflater getDeflater() {
        return def;
    }

    private static class DeflaterFactory {
        protected Deflater newDeflater() {
            return new Deflater(DEFAULT_COMPRESSION, true);
        }
    }

    private static final class Jdk6DeflaterFactory extends DeflaterFactory {
        @Override
        protected Deflater newDeflater() {
            return new Jdk6Deflater(DEFAULT_COMPRESSION, true);
        }
    }
}
