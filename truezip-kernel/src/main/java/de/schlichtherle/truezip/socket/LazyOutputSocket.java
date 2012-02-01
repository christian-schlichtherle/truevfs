/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * A lazy output socket provides proxy output streams which acquire their
 * underlying local target upon the first read access.
 *
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @see     LazyInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class LazyOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    public LazyOutputSocket(@NonNull OutputSocket<? extends E> output) {
        super(output);
    }

    /**
     * Returns a proxy output stream which acquires its underlying output
     * stream upon the first read access.
     *
     * @return A proxy output stream which acquires its underlying output
     *         stream upon the first write access.
     */
    @Override
    public OutputStream newOutputStream() throws IOException {
        return new ProxyOutputStream();
    }

    @NotThreadSafe
    private class ProxyOutputStream extends DecoratingOutputStream {
        ProxyOutputStream() {
            super(null);
        }

        OutputStream getOutputStream() throws IOException {
            final OutputStream out = delegate;
            return null != out ? out : (delegate = getBoundSocket().newOutputStream());
        }

        @Override
        public void write(int b) throws IOException {
            getOutputStream().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            getOutputStream().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            getOutputStream().flush();
        }

        @Override
        public void close() throws IOException {
            final OutputStream out = delegate;
            if (null != out)
                out.close();
        }
    } // ProxyOutputStream
}
