/*
 * Copyright 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @see     LazyInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class LazyOutputSocket<LT extends CommonEntry> extends OutputSocket<LT> {

    private OutputSocketProvider<LT> provider;
    private OutputSocket<? extends LT> socket;
    private LT target;

    public LazyOutputSocket(final OutputSocketProvider<LT> provider,
                            final LT target) {
        if (null == provider || null == target)
            throw new NullPointerException();
        this.provider = provider;
        this.target = target;
    }

    public LazyOutputSocket(final OutputSocket<? extends LT> output) {
        if (null == output)
            throw new NullPointerException();
        this.socket = output;
    }

    protected final OutputSocket<? extends LT> getOutputSocket() throws IOException {
        if (null == socket) {
            socket = provider.getOutputSocket(target);
            provider = null; // support gc!
            target = null;
        }
        return socket.bind(this);
    }

    @Override
    public LT getLocalTarget() throws IOException {
        return getOutputSocket().getLocalTarget();
    }

    @Override
    public CommonEntry getRemoteTarget() throws IOException {
        return getOutputSocket().getRemoteTarget();
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        return new LazyOutputStream();
    }

    private final class LazyOutputStream extends FilterOutputStream {
        LazyOutputStream() {
            super(null);
        }

        OutputStream getOutputStream() throws IOException {
            return null != out ? out : (out = getOutputSocket().newOutputStream());
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
            if (null != out)
                out.close();
        }
    } // class LazyOutputStream
}
