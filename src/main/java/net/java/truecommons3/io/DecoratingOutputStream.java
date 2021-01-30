/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An abstract decorator for an output stream.
 * This class is a clean room alternative to {@link FilterOutputStream}.
 *
 * @see    DecoratingInputStream
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputStream extends OutputStream {

    /** The decorated stream. */
    protected final OutputStream out;

    /**
     * Constructs a new decorating output stream.
     * Closing this stream will close the given stream.
     *
     * @param out the stream to decorate.
     */
    protected DecoratingOutputStream(final OutputStream out) {
        this.out = Objects.requireNonNull(out);
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
        out.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[out=%s]", getClass().getName(), out);
    }
}
