/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.annotation.WillCloseWhenClosed;

/**
 * An inflater input stream which uses a custom {@link Inflater} and provides
 * access to it.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@CleanupObligation
final class ZipInflaterInputStream extends InflaterInputStream {

    private static final InflaterFactory factory = JSE7.AVAILABLE
                        ? new InflaterFactory()        // JDK 7 is OK
                        : new Jdk6InflaterFactory();   // JDK 6 needs fixing

    @CreatesObligation
    ZipInflaterInputStream(@WillCloseWhenClosed DummyByteInputStream in, int size) {
        super(in, factory.newInflater(), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        super.close();
        inf.end();
    }

    /** A factory for {@link Inflater} objects. */
    private static class InflaterFactory {
        protected Inflater newInflater() {
            return new Inflater(true);
        }
    }

    /** A factory for {@link Jdk6Inflater} objects. */
    private static final class Jdk6InflaterFactory extends InflaterFactory {
        @Override
        protected Inflater newInflater() {
            return new Jdk6Inflater(true);
        }
    }
}
