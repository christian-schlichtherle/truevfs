/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io.mock;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.mock.MockControl;
import static de.schlichtherle.truezip.mock.MockControl.check;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating input stream which supports throwing exceptions according to
 * {@link MockControl}.
 * 
 * @see     MockOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockInputStream extends DecoratingInputStream {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockInputStream(@WillCloseWhenClosed InputStream in) {
        super(in);
        in.getClass();
    }

    @Override
    public int read() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.close();
    }

    @Override
    public void mark(int readlimit) {
        check(this, RuntimeException.class);
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        check(this, RuntimeException.class);
        return delegate.markSupported();
    }
}
