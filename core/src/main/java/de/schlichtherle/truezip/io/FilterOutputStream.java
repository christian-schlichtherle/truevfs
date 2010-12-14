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
import java.io.OutputStream;

/**
 * Clean room implementation of its {@link java.io.FilterOutputStream cousin }
 * in the JSE, but optimized for performance and <em>without</em>
 * multithreading support.
 *
 * @see     FilterInputStream
 * @see     SynchronizedOutputStream for a thread-safe filter output stream.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterOutputStream extends OutputStream {

    /** The decorated output stream. */
    protected OutputStream out; // FIXME: Encapsulate this!

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out the output stream to wrap in this decorator.
     */
    protected FilterOutputStream(final OutputStream out) {
        this.out = out;
    }

    @Override
	public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public final void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } finally {
            out.close();
        }
    }
}
