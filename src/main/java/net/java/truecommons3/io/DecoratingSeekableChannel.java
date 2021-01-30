/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * An abstract decorator for a seekable byte channel.
 * <p>
 * Note that sub-classes of this class may maintain their own virtual file
 * pointer.
 * Thus, if you would like to use the decorated seekable byte channel again
 * after you have finished using this seekable byte channel, then you should
 * not assume a particular position of the file pointer of the decorated
 * seekable byte channel.
 *
 * @author Christian Schlichtherle
 */
public abstract class DecoratingSeekableChannel extends AbstractSeekableChannel {

    /** The decorated channel. */
    protected final SeekableByteChannel channel;

    /**
     * Constructs a new decorating seekable channel.
     * Closing this channel will close the given channel.
     *
     * @param channel the channel to decorate.
     */
    protected DecoratingSeekableChannel(final SeekableByteChannel channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
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
    public void close() throws IOException {
        channel.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[channel=%s]", getClass().getName(), channel);
    }
}
