/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except rof compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to rof writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io.rof;

import java.io.IOException;

/**
 * A decorator which synchronizes all access to a {@link ReadOnlyFile}
 * via an object provided to its constructor.
 * <p>
 * This class <em>is</em> thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SynchronizedReadOnlyFile implements ReadOnlyFile {
    /** The object to synchronize on - never {@code null}. */
    protected final Object lock;

    /** The decorated read only file. */
    protected ReadOnlyFile rof;

    /**
     * Constructs a new synchronized read only file.
     * This object will synchronize on itself.
     *
     * @param rof The read only file to wrap in this decorator.
     */
    public SynchronizedReadOnlyFile(final ReadOnlyFile rof) {
        this(rof, null);
    }

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof The input stream to wrap in this decorator.
     * @param lock The object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedReadOnlyFile(final ReadOnlyFile rof, final Object lock) {
        this.rof = rof;
        this.lock = lock != null ? lock : this;
    }

    @Override
    public long length() throws IOException {
        synchronized (lock) {
            return rof.length();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        synchronized (lock) {
            return rof.getFilePointer();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        synchronized (lock) {
            rof.seek(pos);
        }
    }

    public int read() throws IOException {
        synchronized (lock) {
            return rof.read();
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
            return rof.read(b, off, len);
        }
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        synchronized (lock) {
            rof.readFully(b);
        }
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            rof.readFully(b, off, len);
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
        rof.close();
    }
}
