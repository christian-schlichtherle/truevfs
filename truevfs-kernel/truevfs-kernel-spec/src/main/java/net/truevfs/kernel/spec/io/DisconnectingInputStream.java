/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.io;

import de.schlichtherle.truecommons.io.DecoratingInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator which protects the decorated stream from all access
 * unless it's {@linkplain #isOpen() open}.
 *
 * @see    DisconnectingOutputStream
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DisconnectingInputStream extends DecoratingInputStream {

    protected DisconnectingInputStream() { }

    protected DisconnectingInputStream(InputStream in) {
        super(in);
    }

    public abstract boolean isOpen();

    /**
     * Throws an {@link InputClosedException} iff {@link #isOpen()} returns
     * {@code false}.
     * 
     * @throws InputClosedException iff {@link #isOpen()} returns {@code false}.
     */
    protected final void checkOpen() throws InputClosedException {
        if (!isOpen()) throw new InputClosedException();
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkOpen();
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkOpen();
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkOpen();
        return in.available();
    }

    @Override
    public void mark(int readlimit) {
        if (isOpen()) in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        checkOpen();
        in.reset();
    }

    @Override
    public abstract void close() throws IOException;
}
