/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator which protects the decorated channel from all access
 * unless it's {@linkplain #isOpen() open}.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DisconnectingSeekableByteChannel
extends DecoratingSeekableByteChannel {

    protected DisconnectingSeekableByteChannel(@Nullable SeekableByteChannel channel) {
        super(channel);
    }

    @Override
    public abstract boolean isOpen();

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkOpen();
        return delegate.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkOpen();
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkOpen();
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        checkOpen();
        delegate.truncate(size);
        return this;
    }

    @Override
    public abstract void close() throws IOException;
}
