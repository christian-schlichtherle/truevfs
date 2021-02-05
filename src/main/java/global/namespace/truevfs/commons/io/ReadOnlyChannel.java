/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.io;

import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * An abstract decorator for a seekable byte channel which throws a
 * {@link NonWritableChannelException} upon any attempt to modify the decorated
 * seekable byte channel.
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
public class ReadOnlyChannel extends DecoratingSeekableChannel {

    /**
     * Constructs a new read-only channel.
     * Closing this channel closes the decorated channel.
     */
    protected ReadOnlyChannel() {
    }

    /**
     * Constructs a new read-only channel.
     * Closing this channel closes the decorated channel.
     *
     * @param channel the channel to decorate.
     */
    public ReadOnlyChannel(SeekableByteChannel channel) {
        super(channel);
    }

    /**
     * @throws NonWritableChannelException always.
     */
    @Override
    public final int write(ByteBuffer src) throws NonWritableChannelException {
        throw new NonWritableChannelException();
    }

    /**
     * @throws NonWritableChannelException always.
     */
    @Override
    public final SeekableByteChannel truncate(long size)
            throws NonWritableChannelException {
        throw new NonWritableChannelException();
    }
}
