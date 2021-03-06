/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.io;

import net.java.truecommons.io.DecoratingSeekableChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truevfs.kernel.spec.FsThrowManager;

/**
 * A decorating seekable byte channel which supports throwing exceptions
 * according to {@link FsTestConfig}.
 * 
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public final class ThrowingSeekableChannel extends DecoratingSeekableChannel {

    private final FsThrowManager control;

    @CreatesObligation
    public ThrowingSeekableChannel(
            @WillCloseWhenClosed SeekableByteChannel sbc) {
        this(sbc, null);
    }

    @CreatesObligation
    public ThrowingSeekableChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel,
            final @CheckForNull FsThrowManager control) {
        super(channel);
        this.control = null != control
                ? control
                : FsTestConfig.get().getThrowControl();
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
    public boolean isOpen() {
        checkUndeclaredExceptions();
        return channel.isOpen();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkAllExceptions();
        return channel.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkAllExceptions();
        return channel.write(src);
    }

    @Override
    public long position() throws IOException {
        checkAllExceptions();
        return channel.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkAllExceptions();
        channel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        checkAllExceptions();
        return channel.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        checkAllExceptions();
        channel.truncate(size);
        return this;
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        channel.close();
    }
}