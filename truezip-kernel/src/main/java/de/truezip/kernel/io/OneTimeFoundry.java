/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;

/**
 * @param  <S> the type of the stream which gets returned by {@link #stream()}.
 * @param  <C> the type of the channel which gets returned by {@link #channel()}.
 * @author Christian Schlichtherle
 */
public abstract class OneTimeFoundry<S extends Closeable, C extends Channel> {
    private S stream;
    private C channel;

    OneTimeFoundry(final S stream) {
        if (null == (this.stream = stream))
            throw new NullPointerException();
    }

    OneTimeFoundry(final C channel) {
        if (null == (this.channel = channel))
            throw new NullPointerException();
    }

    public S stream() throws IOException {
        final S stream = this.stream;
        if (null == stream)
            throw new IllegalStateException();
        this.stream = null;
        return stream;
    }

    public C channel() throws IOException {
        final C channel = this.channel;
        if (null == channel)
            throw new IllegalStateException();
        this.channel = null;
        return channel;
    }
}
