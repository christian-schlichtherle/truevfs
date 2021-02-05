/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.inst;

import global.namespace.truevfs.commons.cio.DecoratingIoBuffer;
import global.namespace.truevfs.commons.cio.InputSocket;
import global.namespace.truevfs.commons.cio.IoBuffer;
import global.namespace.truevfs.commons.cio.OutputSocket;

import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
public class InstrumentingBuffer<M extends Mediator<M>> extends DecoratingIoBuffer {

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
