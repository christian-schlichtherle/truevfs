/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowControl;
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
 */
@NotThreadSafe
public final class ThrowingOutputStream extends DecoratingOutputStream {

    private final ThrowControl control;

    @CreatesObligation
    public ThrowingOutputStream(@WillCloseWhenClosed OutputStream out) {
        this(out, null);
    }

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
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkAllExceptions();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        out.close();
    }
}