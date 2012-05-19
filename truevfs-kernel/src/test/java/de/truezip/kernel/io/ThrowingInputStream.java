/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowManager;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorating input stream which supports throwing exceptions according to
 * {@link TestConfig}.
 * 
 * @see     ThrowingOutputStream
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public final class ThrowingInputStream extends DecoratingInputStream {

    private final ThrowManager control;

    @CreatesObligation
    public ThrowingInputStream(@WillCloseWhenClosed InputStream in) {
        this(in, null);
    }

    @CreatesObligation
    public ThrowingInputStream( final @WillCloseWhenClosed InputStream in,
                                final @CheckForNull ThrowManager control) {
        super(in);
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
    public int read() throws IOException {
        checkAllExceptions();
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkAllExceptions();
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkAllExceptions();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        checkUndeclaredExceptions();
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        checkAllExceptions();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        checkUndeclaredExceptions();
        return in.markSupported();
    }
}