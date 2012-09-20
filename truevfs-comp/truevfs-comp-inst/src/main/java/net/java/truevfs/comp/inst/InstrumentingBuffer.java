/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.cio.DecoratingIoBuffer;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingBuffer<M extends Mediator<M>>
extends DecoratingIoBuffer {

    protected final M mediator;

    public InstrumentingBuffer(final M mediator, final IoBuffer entry) {
        super(entry);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public InputSocket<? extends IoBuffer> input() {
        return mediator.instrument(this, entry.input());
    }

    @Override
    public OutputSocket<? extends IoBuffer> output() {
        return mediator.instrument(this, entry.output());
    }
}
