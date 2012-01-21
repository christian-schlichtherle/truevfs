/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator which synchronizes all access to a {@link ReadOnlyFile}
 * via an object provided to its constructor.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SynchronizedReadOnlyFile extends DecoratingReadOnlyFile {

    /** The object to synchronize on. */
    protected final Object lock;

    /**
     * Constructs a new synchronized read only file.
     * This object will synchronize on itself.
     *
     * @param rof the read only file to wrap in this decorator.
     * @deprecated This class exists to control concurrent access to a
     *             protected resource, e.g. an {@link InputShop}.
     *             So the lock should never be this object itself.
     */
    public SynchronizedReadOnlyFile(final @Nullable ReadOnlyFile rof) {
        this(rof, null);
    }

    /**
     * Constructs a new synchronized read only file.
     *
     * @param rof the input stream to wrap in this decorator.
     * @param lock the object to synchronize on.
     *        If {@code null}, then this object is used, not the stream.
     */
    public SynchronizedReadOnlyFile(
            final @Nullable ReadOnlyFile rof,
            final @CheckForNull Object lock) {
        super(rof);
        this.lock = null != lock ? lock : this;
    }

    @Override
    public long length() throws IOException {
        synchronized (lock) {
            return delegate.length();
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        synchronized (lock) {
            return delegate.getFilePointer();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        synchronized (lock) {
            delegate.seek(pos);
        }
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
    public void readFully(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            delegate.readFully(b, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            delegate.close();
        }
    }
}
