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
import java.nio.channels.SeekableByteChannel;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for a seekable byte channel.
 * This is optimized for performance and <em>without</em> multithreading
 * support.
 *
 * @author Christian Schlichtherle
 */
@CleanupObligation
public abstract class DecoratingSeekableByteChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel sbc;

    /**
     * Constructs a new decorating seekable byte channel.
     *
     * @param sbc the nullable seekable byte channel to decorate.
     */
    @CreatesObligation
    protected DecoratingSeekableByteChannel(
            final @Nullable @WillCloseWhenClosed SeekableByteChannel sbc) {
        this.sbc = sbc;
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
    public boolean isOpen() {
        return sbc.isOpen();
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
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                sbc);
    }
}