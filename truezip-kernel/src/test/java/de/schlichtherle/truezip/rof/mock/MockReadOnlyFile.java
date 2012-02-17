/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof.mock;

import de.schlichtherle.truezip.mock.MockControl;
import static de.schlichtherle.truezip.mock.MockControl.check;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating read only file which supports throwing exceptions according to
 * {@link MockControl}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockReadOnlyFile extends DecoratingReadOnlyFile {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
        super(rof);
        if (null == rof)
            throw new NullPointerException();
    }

    @Override
    public long length() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.seek(pos);
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
    public void close() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.close();
    }
}
