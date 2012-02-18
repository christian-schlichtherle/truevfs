/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof.mock;

import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
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
public final class MockReadOnlyFile extends DecoratingReadOnlyFile {

    private final ThrowControl control;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockReadOnlyFile(final @WillCloseWhenClosed ReadOnlyFile rof,
                            final @CheckForNull TestConfig config) {
        super(rof);
        if (null == rof)
            throw new NullPointerException();
        if (null == (this.control = (null != config ? config : TestConfig.get()).getControl()))
            throw new NullPointerException();
    }

    private void checkAnyException() throws IOException {
        control.check(this, IOException.class);
        checkUndeclaredException();
    }

    private void checkUndeclaredException() {
        control.check(this, RuntimeException.class);
        control.check(this, Error.class);
    }

    @Override
    public long length() throws IOException {
        checkAnyException();
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        checkAnyException();
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        checkAnyException();
        delegate.seek(pos);
    }

    @Override
    public int read() throws IOException {
        checkAnyException();
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkAnyException();
        return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        checkAnyException();
        delegate.close();
    }
}
