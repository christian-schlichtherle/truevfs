/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Clean room implementation of its {@link java.io.FilterInputStream cousin }
 * in the JSE, but optimized for performance and <em>without</em>
 * multithreading support.
 *
 * @see     FilterOutputStream
 * @see     SynchronizedInputStream for a thread-safe filter input stream.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterInputStream extends InputStream {

    /** The nullable decorated input stream. */
    protected InputStream in;

    /**
     * Constructs a new filter input stream.
     *
     * @param in the input stream to wrap in this decorator.
     */
    protected FilterInputStream(final InputStream in) {
        this.in = in;
    }

    @Override
	public int read() throws IOException {
        return in.read();
    }

    @Override
    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
