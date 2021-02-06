/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An abstract decorator which protects the decorated stream from all access
 * unless it's {@linkplain #isOpen() open}.
 *
 * @see    DisconnectingInputStream
 * @author Christian Schlichtherle
 */
public abstract class DisconnectingOutputStream extends DecoratingOutputStream {

    /**
     * Constructs a new disconnecting output stream.
     * Closing this stream will close the given stream.
     *
     * @param out the stream to decorate.
     */
    protected DisconnectingOutputStream(OutputStream out) { super(out); }

    public abstract boolean isOpen();

    /**
     * Throws an {@link ClosedOutputException} iff {@link #isOpen()} returns
     * {@code false}.
     * 
     * @throws ClosedOutputException iff {@link #isOpen()} returns {@code false}.
     */
    protected final void checkOpen() throws ClosedOutputException {
        if (!isOpen()) throw new ClosedOutputException();
    }

    @Override
    public void write(int b) throws IOException {
        checkOpen();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkOpen();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        out.flush();
    }

    @Override
    public abstract void close() throws IOException;
}
