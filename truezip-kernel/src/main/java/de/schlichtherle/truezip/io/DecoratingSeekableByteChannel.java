/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for a seekable byte channel.
 * This is optimized for performance and <em>without</em> multithreading
 * support.
 *
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@CleanupObligation
public abstract class DecoratingSeekableByteChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel delegate;

    /**
     * Constructs a new decorating seekable byte channel.
     *
     * @param delegate the nullable seekable byte channel to decorate.
     */
    @CreatesObligation
    protected DecoratingSeekableByteChannel(
            final @Nullable @WillCloseWhenClosed SeekableByteChannel delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    /**
     * Throws a {@link ClosedChannelException} iff {@link #isOpen()} returns
     * {@code false}.
     * 
     * @throws ClosedChannelException iff {@link #isOpen()} returns
     *         {@code false}.
     * @since  TrueZIP 7.5.6
     */
    protected final void checkOpen() throws ClosedChannelException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        delegate.truncate(size);
        return this;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]", getClass().getName(), delegate);
    }
}
