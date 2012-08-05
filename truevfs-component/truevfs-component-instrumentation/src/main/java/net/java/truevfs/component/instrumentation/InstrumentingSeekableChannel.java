/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import net.java.truecommons.io.DecoratingSeekableChannel;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
public class InstrumentingSeekableChannel<D extends Director<D>>
extends DecoratingSeekableChannel {
    protected final D director;

    public InstrumentingSeekableChannel(
            final D director,
            final SeekableByteChannel channel) {
        super(channel);
        this.director = Objects.requireNonNull(director);
    }
}
