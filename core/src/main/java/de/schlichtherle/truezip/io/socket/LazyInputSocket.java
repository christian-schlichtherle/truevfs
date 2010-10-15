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

import de.schlichtherle.truezip.io.FilterInputStream;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @see     LazyOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class LazyInputSocket<LT extends CommonEntry> extends InputSocket<LT> {

    private InputSocketProvider<LT> provider;
    private InputSocket<? extends LT> socket;
    private LT target;

    public LazyInputSocket( final InputSocketProvider<LT> provider,
                            final LT target) {
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
        this.target = target;
    }

    public LazyInputSocket( final InputSocket<? extends LT> input) {
        if (null == input)
            throw new NullPointerException();
        this.socket = input;
    }

    protected final InputSocket<? extends LT> getInputSocket()
    throws IOException {
        if (null == socket) {
            socket = provider.getInputSocket(target = getLocalTarget());
            assert socket.getLocalTarget().equals(target) : "interface contract violation!";
            provider = null; // support gc!
            target = null;
        }
        return socket.bind(this);
    }

    @Override
    public LT getLocalTarget() throws IOException {
        if (null != socket)
            return socket.bind(this).getLocalTarget();
        if (null != target)
            return target;
        throw new IllegalStateException("cannot resolve local target!");
    }

    @Override
    public final CommonEntry getRemoteTarget() throws IOException {
        return getInputSocket().getRemoteTarget();
    }

    @Override
    public final InputStream newInputStream() throws IOException {
        return new LazyInputStream();
    }

    private final class LazyInputStream extends FilterInputStream {
        LazyInputStream() {
            super(null);
        }

        InputStream getInputStream() throws IOException {
            return null != in ? in : (in = getInputSocket().newInputStream());
        }

        @Override
        public int read() throws IOException {
            return getInputStream().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getInputStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getInputStream().skip(n);
        }

        @Override
        public int available() throws IOException {
            return getInputStream().available();
        }

        @Override
        public void close() throws IOException {
            if (null != in)
                in.close();
        }

        @Override
        public void mark(int readlimit) {
            try {
                getInputStream().mark(readlimit);
            } catch (IOException ex) {
                // The caller should have called markSupported() before
                // this method. If the underlying input stream isn't
                // available, an IOException should have been thrown there.
                // So most likely the caller did not call markSupported()
                // before, which is a violation of the interface contract.
                throw new AssertionError(ex);
            }
        }

        @Override
        public void reset() throws IOException {
            getInputStream().reset();
        }

        @Override
        public boolean markSupported() {
            try {
                return getInputStream().markSupported();
            } catch (IOException ignored) {
                return false;
            }
        }
    } // class LazyInputStream

    @Override
    public final ReadOnlyFile newReadOnlyFile() throws IOException {
        return new LazyReadOnlyFile();
    }

    private final class LazyReadOnlyFile extends FilterReadOnlyFile {
        LazyReadOnlyFile() {
            super(null);
        }

        ReadOnlyFile getReadOnlyFile() throws IOException {
            return null != rof ? rof : (rof = getInputSocket().newReadOnlyFile());
        }

        @Override
        public long length() throws IOException {
            return getReadOnlyFile().length();
        }

        @Override
        public long getFilePointer() throws IOException {
            return getReadOnlyFile().getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            getReadOnlyFile().seek(pos);
        }

        @Override
        public int read() throws IOException {
            return getReadOnlyFile().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getReadOnlyFile().read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (null != rof)
                rof.close();
        }
    } // class LazyReadOnlyFile
}
