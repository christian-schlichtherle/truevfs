/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorating seekable byte channel which supports throwing exceptions
 * according to {@link TestConfig}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class ThrowingSeekableByteChannel extends DecoratingSeekableByteChannel {

    private final ThrowControl control;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingSeekableByteChannel(
            @WillCloseWhenClosed SeekableByteChannel sbc) {
        this(sbc, null);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    @CreatesObligation
    public ThrowingSeekableByteChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc,
            final @CheckForNull ThrowControl control) {
        super(sbc);
        if (null == sbc)
            throw new NullPointerException();
        this.control = null != control
                ? control
                : TestConfig.get().getThrowControl();
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
    public int read(ByteBuffer dst) throws IOException {
        checkAnyException();
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkAnyException();
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        checkAnyException();
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkAnyException();
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        checkAnyException();
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        checkAnyException();
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        checkUndeclaredException();
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        checkAnyException();
        delegate.close();
    }
}
