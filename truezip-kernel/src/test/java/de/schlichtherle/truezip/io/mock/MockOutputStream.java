/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io.mock;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
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
 * @see     MockInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class MockOutputStream extends DecoratingOutputStream {

    private final ThrowControl control;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockOutputStream(final @WillCloseWhenClosed OutputStream out,
                            final @CheckForNull TestConfig config) {
        super(out);
        if (null == out)
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
    public void write(int b) throws IOException {
        checkAnyException();
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAnyException();
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkAnyException();
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        checkAnyException();
        delegate.close();
    }
}
