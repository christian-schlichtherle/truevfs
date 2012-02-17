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
import de.schlichtherle.truezip.mock.MockControl;
import static de.schlichtherle.truezip.mock.MockControl.check;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating output stream which supports throwing exceptions according to
 * {@link MockControl}.
 * 
 * @see     MockInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockOutputStream extends DecoratingOutputStream {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockOutputStream(@WillCloseWhenClosed OutputStream out) {
        super(out);
        out.getClass();
    }

    @Override
    public void write(int b) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.close();
    }
}
