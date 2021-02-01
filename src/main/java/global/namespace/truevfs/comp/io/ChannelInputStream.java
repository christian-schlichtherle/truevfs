/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * Adapts a {@link SeekableByteChannel} to an input stream.
 * This stream supports marking unless the adapted channel fails to set its
 * position in {@link #markSupported}.
 *
 * @see    ChannelOutputStream
 * @author Christian Schlichtherle
 */
public class ChannelInputStream extends InputStream {

    /** The adapted seekable byte channel. */
    protected final SeekableByteChannel channel;

    private final ByteBuffer single = ByteBuffer.allocate(1);

    /**
     * The position of the last mark.
     * Initialized to {@code -1} to indicate that no mark has been set.
     */
    private long mark = -1;

    /**
     * Constructs a new channel input stream.
     * Closing this stream will close the given channel.
     *
     * @param channel the channel to decorate.
     */
    public ChannelInputStream(final SeekableByteChannel channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public int read() throws IOException {
        single.rewind();
        return 1 == read(single) ? single.get(0) & 0xff : -1;
    }

    @Override
    public final int read(byte[] b) throws IOException {
        return read(ByteBuffer.wrap(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return read(ByteBuffer.wrap(b, off, len));
    }

    @SuppressWarnings("SleepWhileInLoop")
    private int read(ByteBuffer bb) throws IOException {
        if (0 == bb.remaining())
            return 0;
        int read;
        while (0 == (read = channel.read(bb))) {
            try {
                Thread.sleep(50);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0)
            return 0;
        final long pos = channel.position(); // should fail when closed
        final long size = channel.size();
        final long rem = size - pos;
        if (n > rem)
            n = (int) rem;
        channel.position(pos + n);
        return n;
    }

    @Override
    public int available() throws IOException {
        final long avl = channel.size() - channel.position();
        return avl > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) avl;
    }

    @Override
    public void close() throws IOException { channel.close(); }

    @Override
    public void mark(final int readlimit) {
        try {
            mark = channel.position();
        } catch (final IOException ex) {
            mark = -2;
        }
    }

    @Override
    public void reset() throws IOException {
        if (0 > mark)
            throw new IOException(-1 == mark
                    ? "No mark set!"
                    : "mark()/reset() not supported!");
        channel.position(mark);
        mark = -1;
    }

    @Override
    public boolean markSupported() {
        try {
            channel.position(channel.position());
            return true;
        } catch (final IOException ex) {
            mark = -2;
            return false;
        }
    }
}
