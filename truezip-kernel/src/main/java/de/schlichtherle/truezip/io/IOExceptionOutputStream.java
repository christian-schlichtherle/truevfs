/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorating output stream which saves the last {@link IOException}
 * in a {@link #exception protected field} for later use.
 *
 * @since   TrueZIP 7.3.2
 * @author  Christian Schlichtherle
 * @deprecated This class will be removed in TrueZIP 8.
 * @version $Id$
 */
public class IOExceptionOutputStream extends DecoratingOutputStream {

    /** The nullable last I/O exception. */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    protected @CheckForNull IOException exception;

    /**
     * Constructs a new I/O exception output stream.
     *
     * @param out the nullable output stream to decorate.
     */
    protected IOExceptionOutputStream(@Nullable OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            delegate.write(b);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            delegate.write(b, off, len);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            delegate.flush();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
}
