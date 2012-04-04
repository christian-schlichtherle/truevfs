/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which wraps any {@link IOException} in an
 * {@link InputException}.
 * 
 * @author Christian Schlichtherle
 */
public final class InputExceptionStream extends DecoratingInputStream {

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public InputExceptionStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        try {
            return in.read();
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return in.read(b, off, len);
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            return in.skip(n);
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return in.available();
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }

    @Override
    public void reset() throws IOException {
        try {
            in.reset();
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        try {
            in.close();
        } catch (IOException ex) {
            throw new InputException(ex);
        }
    }
}
