/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
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
 * @version $Id$
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
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        checkAllExceptions();
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        checkAllExceptions();
        delegate.seek(pos);
    }

    @Override
    public int read() throws IOException {
        checkAllExceptions();
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        delegate.close();
    }
}
