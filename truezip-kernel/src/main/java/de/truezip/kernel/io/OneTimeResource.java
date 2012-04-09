/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * @param  <S> the type of the stream which gets returned by {@link #newStream()}.
 * @author Christian Schlichtherle
 */
public abstract class OneTimeResource<S extends Closeable> {
    private S stream;
    private SeekableByteChannel channel;

    OneTimeResource(final S stream) {
        if (null == (this.stream = stream))
            throw new NullPointerException();
    }

    OneTimeResource(final SeekableByteChannel channel) {
        if (null == (this.channel = channel))
            throw new NullPointerException();
    }

    public S newStream() throws IOException {
        final S stream = this.stream;
        if (null == stream)
            throw new IllegalStateException();
        this.stream = null;
        return stream;
    }

    public SeekableByteChannel newChannel() throws IOException {
        final SeekableByteChannel channel = this.channel;
        if (null == channel)
            throw new IllegalStateException();
        this.channel = null;
        return channel;
    }
}
