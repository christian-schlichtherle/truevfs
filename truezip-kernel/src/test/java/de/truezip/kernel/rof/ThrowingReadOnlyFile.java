/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowControl;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorating read only file which supports throwing exceptions according to
 * {@link TestConfig}.
 * 
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public final class ThrowingReadOnlyFile extends DecoratingReadOnlyFile {

    private final ThrowControl control;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
        this(rof, null);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingReadOnlyFile(final @WillCloseWhenClosed ReadOnlyFile rof,
                            final @CheckForNull ThrowControl control) {
        super(rof);
        if (null == rof)
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
    public long length() throws IOException {
        checkAllExceptions();
        return rof.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        checkAllExceptions();
        return rof.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        checkAllExceptions();
        rof.seek(pos);
    }

    @Override
    public int read() throws IOException {
        checkAllExceptions();
        return rof.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        return rof.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        rof.close();
    }
}