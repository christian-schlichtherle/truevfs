/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorating output stream which supports throwing exceptions according to
 * {@link TestConfig}.
 * 
 * @see     ThrowingInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class ThrowingOutputStream extends DecoratingOutputStream {

    private final ThrowControl control;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingOutputStream(@WillCloseWhenClosed OutputStream out) {
        this(out, null);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingOutputStream(final @WillCloseWhenClosed OutputStream out,
                                final @CheckForNull ThrowControl control) {
        super(out);
        if (null == out)
            throw new NullPointerException();
        this.control = null != control
                ? control
                : TestConfig.get().getThrowControl();
    }

    private void checkAllExceptions() throws IOException {
        control.check(this, IOException.class);
        checkUndeclaredExceptions();
    }

    private void checkUndeclaredExceptions() {
        control.check(this, RuntimeException.class);
        control.check(this, Error.class);
    }

    @Override
    public void write(int b) throws IOException {
        checkAllExceptions();
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkAllExceptions();
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        delegate.close();
    }
}
