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
public abstract class DecoratingSeekableByteChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel sbc;

    @CreatesObligation
    protected DecoratingSeekableByteChannel(
            final @CheckForNull @WillCloseWhenClosed SeekableByteChannel sbc) {
        this.sbc = sbc;
    }

    @Override
    public boolean isOpen() {
        return sbc.isOpen();
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
        return sbc.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return sbc.write(src);
    }

    @Override
    public long position() throws IOException {
        return sbc.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        sbc.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return sbc.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        sbc.truncate(size);
        return this;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        sbc.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[sbc=%s]",
                getClass().getName(),
                sbc);
    }
}
