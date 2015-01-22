/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.cio.DecoratingIoBufferPool;
import net.java.truecommons.cio.IoBuffer;
import net.java.truecommons.cio.IoBufferPool;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingBufferPool<M extends Mediator<M>>
extends DecoratingIoBufferPool {

    protected final M mediator;

    public InstrumentingBufferPool(
            final M mediator,
            final IoBufferPool pool) {
        super(pool);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public IoBuffer allocate() throws IOException {
        return mediator.instrument(this, pool.allocate());
    }
}
