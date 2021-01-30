/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An abstract decorator for an input stream.
 * This class is a clean room alternative to {@link FilterInputStream}.
 *
 * @see    DecoratingOutputStream
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputStream extends InputStream {

    /** The decorated stream. */
    protected final InputStream in;

    /**
     * Constructs a new decorating input stream.
     * Closing this stream will close the given stream.
     *
     * @param in the stream to decorate.
     */
    protected DecoratingInputStream(final InputStream in) {
        this.in = Objects.requireNonNull(in);
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

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[in=%s]", getClass().getName(), in);
    }
}
