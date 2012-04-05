/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for a seekable byte channel.
 * This is optimized for performance and <em>without</em> multithreading
 * support.
 * <p>
 * Note that sub-classes of this class may implement their own virtual file
 * pointer.
 * Thus, if you would like to use the decorated seekable byte channel again
 * after you have finished using this seekable byte channel, then you should
 * not assume a particular position of the file pointer of the decorated
 * seekable byte channel.
 *
 * @author Christian Schlichtherle
 */
@CleanupObligation
public abstract class DecoratingSeekableChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel channel;

    @CreatesObligation
    protected DecoratingSeekableChannel(
            final @CheckForNull @WillCloseWhenClosed SeekableByteChannel sbc) {
        this.channel = sbc;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    /**
     * Throws a {@link ClosedChannelException} iff {@link #isOpen()} returns
     * {@code false}.
     * 
     * @throws ClosedChannelException iff {@link #isOpen()} returns
     *         {@code false}.
     */
    protected final void checkOpen() throws ClosedChannelException {
        if (!isOpen())
            throw new ClosedChannelException();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        channel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        channel.truncate(size);
        return this;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        channel.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[sbc=%s]",
                getClass().getName(),
                channel);
    }
}
