/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import net.java.truecommons.io.DecoratingSeekableChannel;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
public class InstrumentingSeekableChannel<M extends Mediator<M>>
extends DecoratingSeekableChannel {
    protected final M mediator;

    public InstrumentingSeekableChannel(
            final M mediator,
            final SeekableByteChannel channel) {
        super(channel);
        this.mediator = Objects.requireNonNull(mediator);
    }
}
