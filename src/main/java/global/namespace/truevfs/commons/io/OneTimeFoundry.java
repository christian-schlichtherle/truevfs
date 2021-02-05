/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.io;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Objects;

/**
 * A source or sink which provides a given stream or channel at most once.
 *
 * @param  <S> the type of the stream which gets returned by {@link #stream()}.
 * @param  <C> the type of the channel which gets returned by {@link #channel()}.
 * @author Christian Schlichtherle
 */
public abstract class OneTimeFoundry<S extends Closeable, C extends Channel> {

    private @Nullable S stream;
    private @Nullable C channel;

    OneTimeFoundry(final S stream) {
        this.stream = Objects.requireNonNull(stream);
    }

    OneTimeFoundry(final C channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    public S stream() throws IOException {
        final S stream = this.stream;
        if (null == stream) throw new IllegalStateException();
        this.stream = null;
        return stream;
    }

    public C channel() throws IOException {
        final C channel = this.channel;
        if (null == channel) throw new IllegalStateException();
        this.channel = null;
        return channel;
    }
}
