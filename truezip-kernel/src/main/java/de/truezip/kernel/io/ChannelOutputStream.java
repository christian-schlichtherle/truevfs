/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Adapts a {@link WritableByteChannel} to the {@code OutputStream} interface.
 *
 * @see    SeekableChannelInputStream
 * @author Christian Schlichtherle
 */
@NotThreadSafe
@CleanupObligation
public class ChannelOutputStream extends OutputStream {

    private final ByteBuffer single = ByteBuffer.allocate(1);

    /**
     * The underlying {@link SeekableByteChannel}.
     * All methods in this class throw a {@link NullPointerException} if this
     * hasn't been initialized.
     */
    protected @Nullable WritableByteChannel channel;

    @CreatesObligation
    public ChannelOutputStream(
            final @CheckForNull @WillCloseWhenClosed WritableByteChannel sbc) {
        this.channel = sbc;
    }

    @Override
    public void write(int b) throws IOException {
        single.put(0, (byte) b);
        single.rewind();
        if (1 != channel.write(single))
            throw new IOException("write error");
    }

    @Override
    public final void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len != channel.write(ByteBuffer.wrap(b, off, len)))
            throw new IOException("write error");
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        channel.close();
    }
}
