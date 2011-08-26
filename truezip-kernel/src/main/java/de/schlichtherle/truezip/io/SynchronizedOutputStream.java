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
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which synchronizes all access to an {@link OutputStream}
 * via an object provided to its constructor.
 *
 * @see     SynchronizedInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SynchronizedOutputStream extends DecoratingOutputStream {

    /** The object to synchronize on - never {@code null}. */
    protected final Object lock;

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out the output stream to wrap in this decorator.
     */
    public SynchronizedOutputStream(@Nullable OutputStream out) {
    	this(out, null);
    }

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out the output stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedOutputStream(
            final @Nullable OutputStream out,
            final @CheckForNull Object lock) {
        super(out);
        this.lock = null != lock ? lock : this;
    }

    @Override
	public void write(int b) throws IOException {
        synchronized (lock) {
            delegate.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            delegate.flush();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}
