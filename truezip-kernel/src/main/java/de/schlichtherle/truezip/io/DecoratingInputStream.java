/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an input stream.
 * This is a clean room implementation of its cousin {@link FilterInputStream}
 * in the JSE, but optimized for performance and <em>without</em>
 * multithreading support.
 *
 * @see     DecoratingOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@CleanupObligation
public abstract class DecoratingInputStream extends InputStream {

    /** The nullable decorated input stream. */
    protected @Nullable InputStream delegate;

    /**
     * Constructs a new decorating input stream.
     *
     * @param delegate the nullable input stream to decorate.
     */
    @CreatesObligation
    protected DecoratingInputStream(
            final @Nullable @WillCloseWhenClosed InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final String n = getClass().getName();
        final String d = delegate.toString();
        return new StringBuilder(n.length() + "[delegate=".length() + d.length() + 1)
                .append(n)
                .append("[delegate=")
                .append(d)
                .append(']')
                .toString();
    }
}
