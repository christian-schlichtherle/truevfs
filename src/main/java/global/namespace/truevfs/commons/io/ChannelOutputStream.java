/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * Adapts a {@link WritableByteChannel} to an output stream.
 *
 * @see    ChannelInputStream
 * @author Christian Schlichtherle
 */
public class ChannelOutputStream extends OutputStream {

    private final ByteBuffer single = ByteBuffer.allocate(1);

    /** The adapted nullable writable byte channel. */
    protected final WritableByteChannel channel;

    /**
     * Constructs a new channel output stream.
     * Closing this stream closes the given channel.
     *
     * @param channel the channel to decorate.
     */
    public ChannelOutputStream(final WritableByteChannel channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public void write(int b) throws IOException {
        write((ByteBuffer) single.put(0, (byte) b).rewind());
    }

    @Override
    public final void write(byte[] b) throws IOException {
        write(ByteBuffer.wrap(b));
    }

    @Override
    public void write(final byte[] b, final int off, final int len)
    throws IOException {
        write(ByteBuffer.wrap(b, off, len));
    }

    @SuppressWarnings("SleepWhileInLoop")
    private void write(final ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) {
            if (0 == channel.write(bb)) {
                try {
                    Thread.sleep(50);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt(); // restore
                }
            }
        }
    }

    @Override
    public void flush() throws IOException { }

    @Override
    public void close() throws IOException { channel.close(); }
}
