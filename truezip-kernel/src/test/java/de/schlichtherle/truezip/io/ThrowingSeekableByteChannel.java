/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
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

    private void checkAllExceptions() throws IOException {
        control.check(this, IOException.class);
        checkUndeclaredExceptions();
    }

    private void checkUndeclaredExceptions() {
        control.check(this, RuntimeException.class);
        control.check(this, Error.class);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkAllExceptions();
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkAllExceptions();
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        checkAllExceptions();
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkAllExceptions();
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        checkAllExceptions();
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        checkAllExceptions();
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        checkUndeclaredExceptions();
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        delegate.close();
    }
}