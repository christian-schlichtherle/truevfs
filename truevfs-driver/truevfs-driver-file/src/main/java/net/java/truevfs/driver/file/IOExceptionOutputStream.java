/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import net.java.truecommons.io.DecoratingOutputStream;

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
    IOExceptionOutputStream(@WillCloseWhenClosed OutputStream out) {
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
