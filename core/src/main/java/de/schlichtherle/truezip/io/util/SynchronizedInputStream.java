/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A decorator which synchronizes all access to an {@link InputStream}
 * via an object provided to its constructor.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SynchronizedInputStream extends InputStream {
    /** The object to synchronize on - never {@code null}. */
    protected final Object lock;

    /** The decorated input stream. */
    protected InputStream in;

    /**
     * Constructs a new synchronized input stream.
     * This object will synchronize on itself.
     *
     * @param in The input stream to wrap in this decorator.
     */
    public SynchronizedInputStream(final InputStream in) {
        this(in, null);
    }

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in The input stream to wrap in this decorator.
     * @param lock The object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedInputStream(final InputStream in, final Object lock) {
        this.in = in;
        this.lock = lock != null ? lock : this;
    }

    public int read() throws IOException {
        synchronized (lock) {
            return in.read();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        synchronized (lock) {
            return read(b, 0, b.length);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            return in.read(b, off, len);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            return in.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (lock) {
            return in.available();
        }
    }

    /** Synchronizes on the {@link #lock} and calls {@link #doClose}. */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            doClose();
        }
    }

    /**
     * Closes the underlying input stream.
     * This method is <em>not</em> synchronized!
     */
    protected void doClose() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        synchronized (lock) {
            in.mark(readlimit);
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            in.reset();
        }
    }

    @Override
    public boolean markSupported() {
        synchronized (lock) {
            return in.markSupported();
        }
    }
}
