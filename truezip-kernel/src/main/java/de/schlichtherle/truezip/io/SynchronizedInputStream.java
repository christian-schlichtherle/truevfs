/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link InputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SynchronizedInputStream extends DecoratingInputStream {

    /** The object to synchronize on - never {@code null}. */
    protected final Object lock;

    /**
     * Constructs a new synchronized input stream.
     * This object will synchronize on itself.
     *
     * @param in the input stream to wrap in this decorator.
     */
    public SynchronizedInputStream(@Nullable InputStream in) {
        this(in, null);
    }

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedInputStream(
            final @Nullable InputStream in,
            final @CheckForNull Object lock) {
        super(in);
        this.lock = null != lock ? lock : this;
    }

    @Override
	public int read() throws IOException {
        synchronized (lock) {
            return delegate.read();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            return delegate.read(b, off, len);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            return delegate.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (lock) {
            return delegate.available();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }

    @Override
    public void mark(int readlimit) {
        synchronized (lock) {
            delegate.mark(readlimit);
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            delegate.reset();
        }
    }

    @Override
    public boolean markSupported() {
        synchronized (lock) {
            return delegate.markSupported();
        }
    }
}
