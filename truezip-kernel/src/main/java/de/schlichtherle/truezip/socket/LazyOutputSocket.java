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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
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
