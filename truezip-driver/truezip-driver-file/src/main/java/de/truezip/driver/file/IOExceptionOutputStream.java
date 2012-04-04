/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import de.truezip.kernel.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating output stream which saves the last {@link IOException}
 * in a {@linkplain #exception protected field} for later use.
 *
 * @author Christian Schlichtherle
 */
abstract class IOExceptionOutputStream extends DecoratingOutputStream {

    /** The nullable last I/O exception. */
    @CheckForNull IOException exception;

    /**
     * Constructs a new I/O exception output stream.
     *
     * @param out the nullable output stream to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    IOExceptionOutputStream(@Nullable @WillCloseWhenClosed OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            out.write(b);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            out.write(b, off, len);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            out.flush();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
}