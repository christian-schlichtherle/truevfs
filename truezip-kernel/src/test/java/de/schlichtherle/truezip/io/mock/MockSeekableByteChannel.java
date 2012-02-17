/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io.mock;

import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.mock.MockControl;
import static de.schlichtherle.truezip.mock.MockControl.check;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating seekable byte channel which supports throwing exceptions
 * according to {@link MockControl}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockSeekableByteChannel extends DecoratingSeekableByteChannel {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public MockSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
        super(sbc);
        sbc.getClass();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        check(this, RuntimeException.class);
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        delegate.close();
    }
}
